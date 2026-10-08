"""Pull the two JNI libraries for phone ABIs out of the sherpa Android archive."""
import os
import sys
import tarfile

tar_path, dest = sys.argv[1], sys.argv[2]
wanted = {"libsherpa-onnx-jni.so", "libonnxruntime.so"}
abis = {"arm64-v8a", "armeabi-v7a"}
os.makedirs(dest, exist_ok=True)
found = 0
with tarfile.open(tar_path, "r:bz2") as tar:
    for member in tar.getmembers():
        if not member.isfile():
            continue
        name = member.name.replace("\\", "/")
        base = os.path.basename(name)
        abi = os.path.basename(os.path.dirname(name))
        if base not in wanted or abi not in abis:
            continue
        member.name = f"{abi}/{base}"
        tar.extract(member, dest, filter="data")
        found += 1
if found < 4:
    raise SystemExit(f"expected 4 libraries, extracted {found}")
print(f"extracted {found}")
