from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import urllib.request,hashlib,json,cv2
ROOT=Path(__file__).resolve().parents[1]/"demo-media"/"distinct-videos"
ROOT.mkdir(exist_ok=True)
JOBS=[
 ("Video_Buom_canh_trong.mp4","Glasswinged_butterfly.webm","https://upload.wikimedia.org/wikipedia/commons/6/6e/Glasswinged_butterfly.webm","https://commons.wikimedia.org/wiki/File:Glasswinged_butterfly.webm","Brandon","CC BY 2.0","https://creativecommons.org/licenses/by/2.0/",8),
 ("Video_Ong_tren_hoa.mp4","Bumblebee_on_flowers.webm","https://upload.wikimedia.org/wikipedia/commons/0/04/Bumblebee_on_flowers.webm","https://commons.wikimedia.org/wiki/File:Bumblebee_on_flowers.webm","Роман Рябенко","CC0 1.0","https://creativecommons.org/publicdomain/zero/1.0/",7),
 ("Video_Thac_nuoc.mp4","Water_falls.webm","https://upload.wikimedia.org/wikipedia/commons/8/8e/Water_falls.webm","https://commons.wikimedia.org/wiki/File:Water_falls.webm","Gvignesh493","CC BY-SA 4.0","https://creativecommons.org/licenses/by-sa/4.0/",6)]
def fetch(job):
 name,raw,url,page,author,license,license_url,seconds=job
 source=ROOT/raw
 if not source.exists():
  request=urllib.request.Request(url,headers={"User-Agent":"GalleryApp-demo/1.0 (educational demo media; Wikimedia Commons attribution retained)"})
  with urllib.request.urlopen(request,timeout=60) as response:source.write_bytes(response.read())
 reader=cv2.VideoCapture(str(source));ok,frame=reader.read();assert ok
 h,w=frame.shape[:2];ratio=720/max(h,w);size=(int(w*ratio)//2*2,int(h*ratio)//2*2)
 fps=reader.get(cv2.CAP_PROP_FPS);assert fps>0
 writer=cv2.VideoWriter(str(ROOT/name),cv2.VideoWriter_fourcc(*"mp4v"),20,size);assert writer.isOpened()
 idx=0;output=0
 try:
  while ok and output<seconds*20:
   if idx/fps>=output/20:
    writer.write(cv2.resize(frame,size,interpolation=cv2.INTER_AREA));output+=1
   idx+=1;ok,frame=reader.read()
 finally:reader.release();writer.release()
 assert output>=seconds*20-1
 check=cv2.VideoCapture(str(ROOT/name));ok,frame=check.read();assert ok;check.release()
 print(name,output,"frames")
 return {"file":name,"source_page":page,"download":url,"author":author,"license":license,"license_url":license_url,"changes":"First %s seconds, resized, converted to silent MPEG-4 MP4"%seconds,"sha256":hashlib.sha256((ROOT/name).read_bytes()).hexdigest()}
if __name__=="__main__":
 with ThreadPoolExecutor(max_workers=2) as pool:manifest=list(pool.map(fetch,JOBS))
 (ROOT/"sources.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding="utf-8")
 (ROOT/"ATTRIBUTION.md").write_text("# Video sources and attribution\n\n"+"\n\n".join("- [%s](%s) — %s, [%s](%s). %s."%(x['file'],x['source_page'],x['author'],x['license'],x['license_url'],x['changes']) for x in manifest),encoding="utf-8")
