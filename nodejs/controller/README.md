# Node.js Class Management API

## 프로젝트 개요
이 프로젝트는 **태권도 도장 관리 서비스**를 위한 백엔드 API로, 프로젝트 중 일부인 수업(Class) 관리와 사용자(User) 관리에서만을 포함합니다. `Node.js`와 `Express.js`를 기반으로 개발되었으며, 도장 관리자 및 강사가 데이터를 효율적으로 관리할 수 있도록 설계되었습니다.

---

## 주요 기능
### 1. 수업 관리 (Class Management)
- **생성**: 새로운 수업 추가
- **조회**: 특정 수업 또는 모든 수업 정보 조회
- **수정**: 수업 정보 업데이트
- **삭제**: 기존 수업 삭제

### 2. 사용자 관리 (User Management)
- **회원가입**: 사용자 등록 및 중복 확인
- **로그인**: 사용자 인증 및 JWT 발급
- **비밀번호 및 개인정보 변경**: 사용자의 비밀번호 및 연락처 정보 수정
- **출석 관리**: 출석 데이터 생성 및 조회
- **알림 관리**: 알림 생성 및 사용자 간 소통 지원

---

## 기술 스택
- **Backend**: Node.js, Express.js
- **Database**: MySQL (Sequelize ORM)
- **Authentication**: JSON Web Token (JWT)

---

## API 코드 구성
- `controllers/classController.js`: 수업 관련 API 로직
- `controllers/userController.js`: 사용자 관련 API 로직
- `models/`: Sequelize를 사용한 데이터베이스 모델 정의
- `util/s3.js`: AWS S3 파일 업로드 유틸리티
- `util/redis-util.js`: Redis 관련 유틸리티

---

## 주요 API 경로
### 수업 관리 (Class)
- `POST /api/classes`: 수업 생성
- `GET /api/classes/:id`: 특정 수업 조회
- `PUT /api/classes/:id`: 수업 수정
- `DELETE /api/classes/:id`: 수업 삭제

### 사용자 관리 (User)
- `POST /api/register`: 회원가입
- `POST /api/login`: 로그인
- `PUT /api/user/info`: 개인정보 수정
- `POST /api/attendance`: 출석 생성
- `GET /api/attendance/:id`: 출석 정보 조회
- `GET /api/notification`: 사용자 알림 조회

---

## 참고 사항
- 이 코드는 **포트폴리오**용으로 생성하였으며 일부 코드만을 포함하였습니다.
