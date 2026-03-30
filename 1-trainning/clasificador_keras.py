import tensorflow as tf
import tensorflow_datasets as tfds
import matplotlib.pyplot as plt
import numpy as np
import itertools
from sklearn.metrics import confusion_matrix

# =====================
# CONFIG
# =====================
BATCH_SIZE = 16
IMG_SIZE = 300
EPOCHS = 30
LR = 3e-4
PATIENCE = 7

tf.keras.mixed_precision.set_global_policy("mixed_float16")

AUTOTUNE = tf.data.AUTOTUNE

# =====================
# DATA
# =====================
def preprocess(image, label):
    image = tf.image.resize(image, (IMG_SIZE, IMG_SIZE))
    image = tf.cast(image, tf.float32)
    image = tf.keras.applications.efficientnet.preprocess_input(image)
    return image, label

dataset, info = tfds.load(
    "oxford_iiit_pet",
    split="train",
    as_supervised=True,
    with_info=True
)

NUM_CLASSES = info.features["label"].num_classes

dataset = dataset.shuffle(1000)
train_size = int(0.8 * info.splits["train"].num_examples)

train_ds = dataset.take(train_size).map(preprocess, AUTOTUNE)
val_ds = dataset.skip(train_size).map(preprocess, AUTOTUNE)

train_ds = train_ds.batch(BATCH_SIZE).prefetch(AUTOTUNE)
val_ds = val_ds.batch(BATCH_SIZE).prefetch(AUTOTUNE)

# =====================
# MIXUP
# =====================
def mixup(images, labels, alpha=0.4):
    batch_size = tf.shape(images)[0]

    indices = tf.random.shuffle(tf.range(batch_size))
    shuffled_images = tf.gather(images, indices)
    shuffled_labels = tf.gather(labels, indices)

    labels = tf.one_hot(labels, NUM_CLASSES)
    shuffled_labels = tf.one_hot(shuffled_labels, NUM_CLASSES)

    lam = tf.random.uniform([], 0, 1)

    images = lam * images + (1 - lam) * shuffled_images
    labels = lam * labels + (1 - lam) * shuffled_labels

    return images, labels

# =====================
# MODEL
# =====================
base_model = tf.keras.applications.EfficientNetB3(
    include_top=False,
    weights="imagenet",
    input_shape=(IMG_SIZE, IMG_SIZE, 3)
)

base_model.trainable = False

x = base_model.output
x = tf.keras.layers.GlobalAveragePooling2D()(x)
x = tf.keras.layers.BatchNormalization()(x)
outputs = tf.keras.layers.Dense(NUM_CLASSES)(x)

model = tf.keras.Model(base_model.input, outputs)

# =====================
# LOSS / OPTIMIZER
# =====================
loss_fn = tf.keras.losses.CategoricalCrossentropy(
    from_logits=True,
    label_smoothing=0.1
)

lr_schedule = tf.keras.optimizers.schedules.CosineDecayRestarts(
    initial_learning_rate=LR,
    first_decay_steps=1000,
    t_mul=2.0
)

optimizer = tf.keras.optimizers.AdamW(
    learning_rate=lr_schedule,
    weight_decay=1e-4
)

# =====================
# METRICS
# =====================
train_acc = tf.keras.metrics.CategoricalAccuracy()
val_acc = tf.keras.metrics.SparseCategoricalAccuracy()

def plot_confusion_matrix(cm, class_names):
    plt.figure(figsize=(10, 8))
    plt.imshow(cm, cmap=plt.cm.Blues)
    plt.title("Confusion Matrix")
    plt.colorbar()

    tick_marks = np.arange(len(class_names))
    plt.xticks(tick_marks, class_names, rotation=90)
    plt.yticks(tick_marks, class_names)

    cm_norm = cm.astype('float') / cm.sum(axis=1)[:, np.newaxis]
    thresh = cm_norm.max() / 2.

    for i, j in itertools.product(range(cm.shape[0]), range(cm.shape[1])):
        plt.text(j, i, f"{cm_norm[i, j]:.2f}",
                 ha="center",
                 color="white" if cm_norm[i, j] > thresh else "black")

    plt.tight_layout()
    plt.ylabel("True")
    plt.xlabel("Pred")
    plt.savefig("confusion_matrix.png")
    plt.show()

    # =====================
# TRAIN STEP
# =====================
@tf.function
def train_step(images, labels):
    images, labels = mixup(images, labels)

    with tf.GradientTape() as tape:
        logits = model(images, training=True)
        loss = loss_fn(labels, logits)

    grads = tape.gradient(loss, model.trainable_variables)
    optimizer.apply_gradients(zip(grads, model.trainable_variables))

    train_acc.update_state(labels, logits)

    return loss

# =====================
# VAL STEP
# =====================
@tf.function
def val_step(images, labels):
    logits = model(images, training=False)
    loss = loss_fn(tf.one_hot(labels, NUM_CLASSES), logits)

    val_acc.update_state(labels, logits)

    return loss

# =====================
# TRAIN LOOP
# =====================
history = {
    "train_loss": [],
    "val_loss": [],
    "train_acc": [],
    "val_acc": []
}
best_val_acc = 0.0
best_val_loss = float("inf") 
patience_counter = 0

for epoch in range(EPOCHS):
    if epoch == 5:
        print("Descongelando backbone para fine-tuning...")
        base_model.trainable = True

    train_acc.reset_state() 
    val_acc.reset_state()

    train_loss = []
    for images, labels in train_ds: 
      loss = train_step(images, labels) 
      train_loss.append(loss) 
    
    val_loss = [] 
    for images, labels in val_ds: 
      loss = val_step(images, labels) 
      val_loss.append(loss)

    train_loss = tf.reduce_mean(train_loss) 
    val_loss = tf.reduce_mean(val_loss)

    train_accuracy = train_acc.result().numpy()
    val_accuracy = val_acc.result().numpy()

    history["train_loss"].append(train_loss.numpy())
    history["val_loss"].append(val_loss.numpy())
    history["train_acc"].append(train_accuracy)
    history["val_acc"].append(val_accuracy)

    if val_accuracy > best_val_acc:
        best_val_acc = val_accuracy

    print(f"\nEpoch {epoch+1}/{EPOCHS}")
    print(f"Train Loss: {train_loss:.4f} | Train Acc: {train_accuracy:.4f}")
    print(f"Val Loss: {val_loss:.4f} | Val Acc: {val_accuracy:.4f}")

    if val_loss < best_val_loss:
        best_val_loss = val_loss
        model.save("best_model_efficientnet_b3.keras")
        print("Nuevo mejor modelo guardado")
        patience_counter = 0
    else:
        patience_counter += 1
        print(f"Early stopping counter: {patience_counter}")
        if patience_counter >= PATIENCE:
            print("Early stopping activado")
            break
print (f"\nEntrenamiento completado, mejor accuracy: {best_val_acc:.4f}")



# =====================
# PLOTS
# =====================
epochs_range = range(len(history["train_loss"]))

plt.figure(figsize=(12,5))

plt.subplot(1,2,1)
plt.plot(epochs_range, history["train_loss"], label="Train Loss")
plt.plot(epochs_range, history["val_loss"], label="Val Loss")
plt.legend()
plt.title("Loss")

plt.subplot(1,2,2)
plt.plot(epochs_range, history["train_acc"], label="Train Acc")
plt.plot(epochs_range, history["val_acc"], label="Val Acc")
plt.legend()
plt.title("Accuracy")

plt.savefig("training_plot.png")
plt.show()


# =====================
# CONFUSION MATRIX
# =====================
y_true = []
y_pred = []

for images, labels in val_ds:
    logits = model(images, training=False)
    preds = tf.argmax(logits, axis=1)

    y_true.extend(labels.numpy())
    y_pred.extend(preds.numpy())

cm = confusion_matrix(y_true, y_pred)
class_names = info.features["label"].names

plot_confusion_matrix(cm, class_names)