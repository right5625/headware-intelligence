import os
from fastapi import APIRouter, File, HTTPException, UploadFile, WebSocket, Depends, status
from fastapi.responses import FileResponse
from pydantic import BaseModel
from typing import List
from starlette.websockets import WebSocketDisconnect
from sqlalchemy.orm import Session
import datetime
from db.db_connection import get_db
from db.models import Accident, UserEmployee
from fcm_notification import fcm_function


router = APIRouter(prefix="/accident")


# 사고 발생 Json 구조
class Accident_Json(BaseModel):
    category: str
    date: List[int] = []
    time: List[int] = []
    latitude: float
    longitude: float
    work_id: str
    victim_id: str

# Websocket 접속 매니저


class ConnectionManager:
    def __init__(self):
        self.active_connections = {}

    async def connect(self, work_id: str, websocket: WebSocket):
        await websocket.accept()
        if work_id in self.active_connections:
            self.active_connections[work_id].append(websocket)
        else:
            self.active_connections[work_id] = [websocket]

    def disconnect(self, work_id: str, websocket: WebSocket):
        self.active_connections[work_id].remove(websocket)
        if not self.active_connections[work_id]:
            del self.active_connections[work_id]

    async def broadcast(self, work_id: str, message: str):
        if work_id in self.active_connections:
            for connection in self.active_connections[work_id]:
                await connection.send_text(message)


# 사고 발생시 데이터를 받아오고, 이를 DB에 저장하는 방식
@router.post("/upload", status_code=status.HTTP_200_OK)
def post_accident(accident: Accident_Json, db: Session = Depends(get_db)):
    db_accident = Accident(date=datetime.date(year=accident.date[0], month=accident.date[1], day=accident.date[2]),
                           time=datetime.time(
                               hour=accident.time[0], minute=accident.time[1], second=accident.time[2]),
                           latitude=accident.latitude,
                           longitude=accident.longitude,
                           work_id=accident.work_id,
                           victim_id=accident.victim_id,
                           category=accident.category)
    db.add(db_accident)
    db.commit()
    user = db.query(UserEmployee).filter(
        UserEmployee.id == accident.victim_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="사용자를 찾을 수 없습니다.")
    fcm_function.fcm_send_messaging(accident.work_id, accident.victim_id, db)
    return {"status": "success"}


@router.post("/upload_image")
async def upload_image(file: UploadFile = File(...)):
    try:
        # 이미지를 저장할 디렉토리 경로 설정
        save_path = "./accident/uploaded_images/"

        # 저장할 디렉토리가 존재하지 않는 경우 생성
        if not os.path.exists(save_path):
            os.makedirs(save_path)

        # 파일 경로 설정
        file_path = os.path.join(save_path, file.filename)

        # 이미지 파일을 서버에 저장
        with open(file_path, "wb") as image_file:
            image_file.write(await file.read())

        # 성공적으로 저장되었다는 응답 반환
        return {"message": "File uploaded successfully", "filename": file.filename}
    except Exception as e:
        # 오류 발생 시 오류 메시지 반환
        return {"message": str(e)}

manager = ConnectionManager()


@router.websocket("/ws/{work_id}/{user_id}")
async def websocket_endpoint(websocket: WebSocket, work_id: str, user_id: str):
    await manager.connect(work_id, websocket)  # client websocket 접속 허용
    try:
        while True:
            data = await websocket.receive_text()  # client 메시지 수신 대기
            await manager.broadcast(work_id, f"{user_id}:{data}")
    except WebSocketDisconnect:
        manager.disconnect(work_id, websocket)


@router.get('/get_image/{victim}/{manager}')
async def get_image(victim: str, manager: str):
    image_path = os.path.join(
        './accident/uploaded_images/', victim + '_' + manager + '.jpg')
    if os.path.exists(image_path):
        return FileResponse(image_path)
    raise HTTPException(status_code=404, detail='image not found')


@router.get("/emergency")
async def emergency_call(work_id: str, user_id: str, db: Session = Depends(get_db)):
    fcm_function.fcm_send_emergency(work_id, user_id, db)
