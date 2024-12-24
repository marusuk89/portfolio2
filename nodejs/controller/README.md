# Node.js Class Management API

## 프로젝트 개요
이 프로젝트는 **태권도 도장 관리 서비스**를 위한 백엔드 API로, 수업(Class) 관리와 공지사항 관리를 포함합니다. `Node.js`와 `Express.js`를 기반으로 개발되었으며, 도장 관리자 및 강사가 데이터를 효율적으로 관리할 수 있도록 설계되었습니다.

## 주요 기능
- **수업 관리**: 수업 생성, 조회, 수정, 삭제 기능 제공.
- **공지사항 관리**: 공지 생성, 조회, 수정, 삭제 및 댓글 기능 제공.
- **파일 관리**: AWS S3를 사용한 이미지 및 파일 업로드/삭제 기능.
- **JWT 인증**: JSON Web Token을 사용한 사용자 인증 및 권한 부여.

## 기술 스택
- **Backend**: Node.js, Express.js
- **Database**: MySQL (Sequelize ORM)
- **File Storage**: AWS S3
- **Authentication**: JSON Web Token (JWT)

## API 코드 구성
- `controllers/classController.js`: 수업 관련 API 로직
- `controllers/noticeController.js`: 공지사항 관련 API 로직
- `models/`: Sequelize를 사용한 데이터베이스 모델 정의
- `util/s3.js`: AWS S3와의 연동 로직 구현
- `apitest.http`: REST Client를 이용한 API 테스트 샘플 요청 파일

## 주요 API 경로
### 수업 관리
- `POST /api/classes`: 수업 생성
- `GET /api/classes/:id`: 수업 조회
- `PUT /api/classes/:id`: 수업 수정
- `DELETE /api/classes/:id`: 수업 삭제

### 공지사항 관리
- `POST /api/notices`: 공지 생성
- `GET /api/notices/:id`: 공지 조회
- `PUT /api/notices/:id`: 공지 수정
- `DELETE /api/notices/:id`: 공지 삭제
- `POST /api/notices/:id/replies`: 댓글 생성

## 참고 사항
- **보안**: 민감한 정보는 `.env` 파일로 관리하며, 해당 파일은 제공되지 않습니다.
- **실행 환경**: 이 프로젝트는 **코드 리뷰 및 참고용**으로 제공되며, 실제 실행은 지원하지 않습니다.
