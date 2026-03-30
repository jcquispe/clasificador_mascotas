import tensorflow as tf
import onnxruntime as ort
import numpy as np
import cv2

model_tf = tf.keras.models.load_model("best_model_efficientnet_b3.keras", compile=False)

session = ort.InferenceSession("best_model_efficientnet_b3.onnx")
input_name = session.get_inputs()[0].name
output_name = session.get_outputs()[0].name

class_names = ["Abyssinian",
"American Bulldog",
"American Pit Bull Terrier",
"Basset Hound",
"Beagle",
"Bengal",
"Birman",
"Bombay",
"Boxer",
"British Shorthair",
"Chihuahua",
"Egyptian Mau",
"English Cocker Spaniel",
"English Setter",
"German Shorthaired",
"Great Pyrenees",
"Havanese",
"Japanese Chin",
"Keeshond",
"Leonberger",
"Maine Coon",
"Miniature Pinscher",
"Newfoundland",
"Persian",
"Pomeranian",
"Pug",
"Ragdoll",
"Russian Blue",
"Saint Bernard",
"Samoyed",
"Scottish Terrier",
"Shiba Inu",
"Siamese",
"Sphynx",
"Staffordshire Bull Terrier",
"Wheaten Terrier",
"Yorkshire Terrier"]

def preprocess(img_path):
    img = cv2.imread(img_path)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = tf.convert_to_tensor(img)
    img = tf.image.resize(img, (300, 300))
    img = tf.cast(img, tf.float32)
    img = tf.keras.applications.efficientnet.preprocess_input(img)
    img = tf.expand_dims(img, axis=0)
    return img.numpy()

img_path = "chihuahua.webp"
img_np = preprocess(img_path)

logits_tf = model_tf(img_np)
pred_tf = tf.argmax(logits_tf, axis=1).numpy()[0]

logits_onnx = session.run([output_name], {input_name: img_np})[0]
pred_onnx = np.argmax(logits_onnx, axis=1)[0]

print("TensorFlow:", class_names[pred_tf])
print("ONNX:", class_names[pred_onnx])