import tensorflow as tf
import tf2onnx

tf.keras.mixed_precision.set_global_policy("float32")

model = tf.keras.models.load_model("best_model_efficientnet_b3.keras", compile=False)

inputs = tf.keras.Input(shape=(300, 300, 3), dtype=tf.float32)
outputs = model(inputs)
model_fp32 = tf.keras.Model(inputs, outputs)

spec = (tf.TensorSpec((None, 300, 300, 3), tf.float32, name="input"),)
model_proto, _ = tf2onnx.convert.from_keras(model_fp32, input_signature=spec)

with open("best_model_efficientnet_b3.onnx", "wb") as f:
    f.write(model_proto.SerializeToString())

print("Modelo convertido correctamente")