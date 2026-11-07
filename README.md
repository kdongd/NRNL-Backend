
#  NO RUN NO LIFE (NRNL)

> **🔥 마음을 불태워라 🔥**
>
> 운동 기록(러닝, 복싱)을 관리하고, 개인 맞춤형 AI 코칭과 사용자 간 소통을 지원하는 운동 기록·커뮤니티 서비스

---

## 📌 프로젝트 소개

**NRNL (No Run No Life)** 은 러닝과 복싱을 중심으로 운동 기록, 컨디션 관리, AI 코칭, 커뮤니티 기능을 제공하는 웹 서비스입니다.

- 🏃 **운동 기록** — 러닝·복싱 기록 등록 및 관리
- 📊 **운동 통계** — 운동 이력과 통계 시각화
- 🤖 **AI 코칭** — 운동 기록과 컨디션을 기반으로 맞춤형 피드백 제공
- 💬 **운동 커뮤니티** — 게시글·댓글·좋아요를 통한 사용자 간 소통

---

## 🛠 기술 스택

### Backend

![Java](https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)

### Database

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-3FCF8E?style=for-the-badge&logo=supabase&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)

### Tools & CI

![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white)

---

## ✅ 주요 구현 기능

| 구분 | 주요 기능 |
|------|-----------|
| 🔐 회원 인증 | 회원가입, 로그인, JWT 인증, 토큰 재발급 및 로그아웃 |
| 👤 회원 프로필 | 닉네임 관리, 신체 정보, 운동 경력 및 목표 설정 |
| 🏃 운동 기록 | 러닝·복싱 기록 CRUD, 운동 날짜 및 메모 관리 |
| ❤️ 운동 컨디션 | 운동 강도(RPE), 몸 상태, 통증 부위 및 강도 기록 |
| 📅 운동 캘린더 | 날짜별 운동 기록 조회 및 운동 이력 관리 |
| 📊 운동 통계 | 운동 횟수, 운동 시간, 종목별 통계 |
| 🤖 AI 코칭 | 최근 운동 기록과 컨디션 분석을 통한 맞춤형 피드백 |
| 📝 커뮤니티 게시글 | 게시글 CRUD, 제목·닉네임 검색, 페이징 및 정렬 |
| 💬 커뮤니티 댓글 | 댓글 CRUD, 게시글별 댓글 조회, 내 댓글 조회 |
| ❤️ 커뮤니티 좋아요 | 게시글·댓글 좋아요 등록, 취소, 수 및 상태 조회 |

---

## 🔐 인증 및 보안

- Spring Security + JWT 기반 인증
- BCrypt를 이용한 비밀번호 암호화
- Access Token 및 Refresh Token 관리
- 회원별 운동 기록 접근 권한 검증
- 게시글·댓글 작성자 수정·삭제 권한 검증
- 좋아요 중복 등록 방지 및 로그인 회원 기준 취소 처리

---

## 📁 프로젝트 구조

```text
src/main/
├── java/com/kdongdexample/norunnolifeexample/
│   ├── config/        # Spring 및 보안 설정
│   ├── controller/    # REST API
│   ├── domain/        # JPA 엔티티
│   ├── dto/           # 요청·응답 데이터
│   ├── exception/     # 예외 처리
│   ├── repository/    # 데이터베이스 접근
│   ├── security/      # JWT 인증 및 보안
│   └── service/       # 비즈니스 로직
└── resources/
    └── db/migration/  # Flyway 마이그레이션
```

---

## 📡 주요 API

| 구분 | API | 설명 |
|------|-----|------|
| 인증 | `/auth/**` | 회원가입, 로그인, 토큰 관리 |
| 회원 | `/users/**` | 회원 정보 및 프로필 관리 |
| 운동 | `/workouts/**` | 운동 기록 및 컨디션 관리 |
| 캘린더 | `/calendar/**` | 날짜별 운동 기록 |
| 통계 | `/stats/**` | 운동 통계 조회 |
| AI 코칭 | `/coaching/**` | 운동 분석 및 코칭 |
| 게시글 | `/community/posts/**` | 게시글 CRUD 및 검색 |
| 댓글 | `/community/posts/{postId}/comments/**` | 댓글 CRUD |
| 좋아요 | `/community/posts/{postId}/likes` | 게시글 좋아요 |
| 댓글 좋아요 | `/community/posts/{postId}/comments/{commentId}/likes` | 댓글 좋아요 |

API 상세 명세는 **Swagger UI (OpenAPI)** 를 통해 확인할 수 있습니다.

---

## 🧪 테스트 및 CI

- JUnit 5, Mockito 기반 단위 테스트
- MockMvc 기반 REST API 및 인증·인가 테스트
- GitHub Actions를 통한 Gradle 빌드 및 테스트 자동화
- Flyway를 이용한 PostgreSQL 스키마 버전 관리

---

## 🔗 관련 저장소

- **Frontend:** [NRNL-frontend](https://github.com/kdongd/NRNL-frontend)

---
