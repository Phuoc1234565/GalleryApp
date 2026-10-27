"""Archived experiment only; current gallery videos use distinct real footage."""
from pathlib import Path
import cv2
import numpy as np
import hashlib,json
SOURCES=Path(__file__).resolve().parents[1]/"demo-media"/"fresh"
ROOT=SOURCES/"photo-motion-archive"
ROOT.mkdir(exist_ok=True)
JOBS=[("Moi_Bien_binh_minh.png","Moi_Video_Bien_binh_minh.mp4",6),
      ("Moi_Pho_dem_sau_mua.png","Moi_Video_Pho_dem.mp4",7),
      ("Moi_Meo_ben_cua_so.png","Moi_Video_Meo.mp4",5)]
for source,name,seconds in JOBS:
 image=cv2.imread(str(SOURCES/source));assert image is not None
 h,w=image.shape[:2];fps=20
 writer=cv2.VideoWriter(str(ROOT/name),cv2.VideoWriter_fourcc(*"mp4v"),fps,(720,540))
 assert writer.isOpened(),"Video encoder unavailable"
 try:
  for frame in range(seconds*fps):
   t=frame/(seconds*fps-1);zoom=1.05+.12*t
   crop_w,crop_h=int(w/zoom),int(h/zoom)
   x=int((w-crop_w)*t);y=int((h-crop_h)*(.5+.25*np.sin(t*np.pi)))
   writer.write(cv2.resize(image[y:y+crop_h,x:x+crop_w],(720,540),interpolation=cv2.INTER_AREA))
 finally:writer.release()
 reader=cv2.VideoCapture(str(ROOT/name));ok,frame=reader.read()
 assert ok and frame.shape[:2]==(540,720)
 assert int(reader.get(cv2.CAP_PROP_FRAME_COUNT))==seconds*fps
 reader.release();print(name,seconds,"seconds")
manifest=[]
for file in sorted(ROOT.iterdir()):
 if file.suffix.lower() in {".png",".mp4"}:
  manifest.append({"file":file.name,"bytes":file.stat().st_size,"sha256":hashlib.sha256(file.read_bytes()).hexdigest(),"type":"AI generated photograph" if file.suffix==".png" else "silent camera-movement video made from AI photograph"})
(ROOT/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding="utf-8")
(ROOT/"import-files.txt").write_text("\n".join(item["file"] for item in manifest)+"\n",encoding="utf-8")
