# 🥊 NO RUN NO LIFE
> 기록이 실력을 만든다. 러닝과 복싱 기록을 남기는 웹 서비스.

---

## 📌 프로젝트 소개
- 러닝과 복싱, 두 종목의 기록을 한 곳에서 관리하는 Spring Boot REST API 서버
- Spring MVC, JPA를 단계별로 적용하며 처음부터 직접 만들어가는 프로젝트
- 매주 멘토 리뷰를 받으며 진행한 6주 과정의 결과물
- 6주 과정 종료 후, 이메일/비밀번호 로그인과 Google OAuth2 로그인을 추가하며 "누구나 조회 가능한 CRUD 서버"에서 "회원별로 자기 기록만 관리하는 서비스"로 구조를 확장 중

---

## 🛠 기술 스택
| 분류 | 기술                          |
|------|-----------------------------|
| Language | Java 17                     |
| Framework | Spring Boot 3.5             |
| API | REST API, springdoc-openapi (Swagger UI) |
| Build | Gradle                      |
| DB | MySQL (운영) / MySQL Testcontainers, H2 (테스트) |
| ORM | Spring Data JPA / Hibernate |
| 인증/보안 | Spring Security, JJWT 0.12.6 (JWT 발급/검증), BCrypt |
| 소셜 로그인 | Google Identity Services + google-api-client 2.9.0 (ID 토큰 서버 검증) |
| CI | GitHub Actions               |



---
 
## ⚙️ 실행 방법

로컬에 MySQL이 떠 있어야 하고, `norunnolife` 데이터베이스와 접속 계정이 필요합니다. `spring.datasource.username`/`password`는 `application.yml`에서 환경변수 `DB_USERNAME`/`DB_PASSWORD`로 주입받으므로, 실행 전에 값을 설정해야 합니다. `JWT_SECRET`은 기본값이 없어 반드시 직접 설정해야 하며(미설정 시 서버가 부팅되지 않습니다), `GOOGLE_CLIENT_ID`는 `application.yml`에 로컬 개발용 플레이스홀더가 들어 있어 생략해도 서버는 뜨지만 실제 Google 로그인은 동작하지 않습니다.

    export DB_USERNAME=your_db_user
    export DB_PASSWORD=your_db_password
    export JWT_SECRET=your_jwt_secret          # 필수, 미설정 시 서버 부팅 실패
    export GOOGLE_CLIENT_ID=your_google_client_id   # 선택, Google 로그인 테스트 시 필수
    ./gradlew bootRun

서버 실행 후 `http://localhost:8080` 에서 API 사용 가능
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

H2 콘솔(`http://localhost:8080/h2-console`)도 활성화되어 있지만, 실제 데이터소스는 MySQL입니다. 콘솔에서 뭔가 조회하려면 접속 화면에서 JDBC URL을 MySQL 접속 정보(`jdbc:mysql://localhost:3306/norunnolife`)로 직접 바꿔 입력해야 하며, 기본값(H2 임베디드 URL) 그대로는 앱 데이터가 보이지 않습니다.

`/auth/**`를 제외한 모든 엔드포인트(`/workouts/**` 포함)는 이제 인증이 필요합니다. `/auth/signup` → `/auth/login`(또는 `/auth/google`)으로 먼저 `accessToken`을 발급받아 `Authorization: Bearer <accessToken>` 헤더로 호출해야 합니다. 다만 현재 springdoc에 Bearer 인증 스킴(SecurityScheme)이 별도로 설정돼 있지 않아 Swagger UI에는 토큰 입력 UI가 없습니다 — 인증이 필요한 API는 curl/Postman 등으로 헤더를 직접 넣어 테스트해야 합니다.

**테스트 실행에는 위 환경변수가 필요 없습니다.** `src/test/resources/application.yml`이 `JWT_SECRET`/`spring.datasource.username`/`spring.datasource.password`를 테스트 전용 더미 값으로 미리 채워두기 때문에(`ddl-auto`, `cors.allowed-origins` 등 나머지 설정도 `src/main/resources/application.yml`을 그대로 복사해서 함께 포함 — Gradle 테스트 클래스패스에서 `src/test/resources`가 `src/main/resources`보다 먼저 잡혀 `application.yml`이 병합이 아니라 통째로 대체되기 때문에 필요한 값만 넣으면 나머지가 사라짐), 셸에 아무것도 export하지 않은 상태로 `./gradlew test`나 IntelliJ 테스트 실행을 바로 돌려도 됩니다. 단 이 값들은 테스트 전용이며 `bootRun`(실제 앱 실행)에는 적용되지 않으므로, 위 env 설정은 여전히 필요합니다.

---

## 📁 프로젝트 구조

    src/main/java/com/kdongdexample/norunnolifeexample
    ├── config
    │   ├── WebConfig.java
    │   ├── AuditingConfig.java
    │   └── SecurityConfig.java
    ├── controller
    │   ├── WorkoutController.java
    │   └── AuthController.java
    ├── domain
    │   ├── Workout.java
    │   ├── RunningWorkout.java
    │   ├── BoxingWorkout.java
    │   ├── WorkoutDetail.java
    │   ├── WorkoutType.java
    │   ├── TechniqueType.java
    │   ├── User.java
    │   ├── UserRole.java
    │   └── AuthProvider.java
    ├── dto
    │   ├── WorkoutForm.java
    │   ├── WorkoutDetailForm.java
    │   ├── WorkoutResponse.java
    │   ├── WorkoutDetailResponse.java
    │   ├── WorkoutSummaryResponse.java
    │   ├── WorkoutTypeFields.java
    │   ├── PageResponse.java
    │   ├── WorkoutStatByType.java
    │   ├── WorkoutMonthlyStat.java
    │   ├── SignupRequest.java
    │   ├── LoginRequest.java
    │   ├── GoogleLoginRequest.java
    │   └── TokenResponse.java
    ├── exception
    │   ├── ErrorResponse.java
    │   ├── GlobalExceptionHandler.java
    │   ├── WorkoutNotFoundException.java
    │   ├── InvalidSortPropertyException.java
    │   ├── WorkoutTypeMismatchException.java
    │   ├── EmailAlreadyExistsException.java
    │   ├── InvalidCredentialsException.java
    │   ├── InvalidGoogleTokenException.java
    │   ├── GoogleVerificationUnavailableException.java
    │   └── AuthenticatedUserNotFoundException.java
    ├── repository
    │   ├── WorkoutRepository.java
    │   ├── WorkoutQueryRepository.java
    │   ├── JpaWorkoutRepository.java
    │   ├── JpaWorkoutRepositoryAdapter.java
    │   ├── WorkoutSpecifications.java
    │   └── UserRepository.java
    ├── security
    │   ├── JwtTokenProvider.java
    │   ├── JwtAuthenticationFilter.java
    │   ├── TokenStatus.java
    │   ├── CustomAuthenticationEntryPoint.java
    │   └── GoogleIdTokenValidator.java
    └── service
        ├── WorkoutService.java
        └── AuthService.java

    src/test/java/com/kdongdexample/norunnolifeexample
    ├── controller
    │   ├── WorkoutControllerTest.java
    │   └── AuthControllerTest.java
    ├── domain
    │   ├── WorkoutTest.java
    │   ├── WorkoutAuditingTest.java
    │   └── UserTest.java
    ├── repository
    │   ├── WorkoutRepositoryTest.java
    │   ├── JpaWorkoutRepositoryAdapterTest.java
    │   └── JpaWorkoutRepositoryMySQLTest.java
    ├── security
    │   └── JwtTokenProviderTest.java
    ├── service
    │   ├── WorkoutServiceTest.java
    │   └── AuthServiceTest.java
    └── NorunnolifeexampleApplicationTests.java

    src/test/resources
    └── application.yml   # 테스트 전용 설정 — JWT_SECRET 등 더미값, main의 application.yml을 셸 환경변수 없이도 재현

    .github/workflows/ci.yml

---

## 📦 설계 의도

### 1) Controller
- `@RestController`로 JSON 응답
- `@Valid`로 요청 데이터 검증
- `ResponseEntity`로 HTTP 상태코드 명시적 제어 (등록 201 + `Location` 헤더, 조회/수정 200, 삭제 204)
- CORS는 `WebConfig`(`WebMvcConfigurer`)에서 전역으로 처리, origin은 `application.yml`의 `cors.allowed-origins`로 설정화
  (`@CrossOrigin` 어노테이션 속성에서는 SpEL 기반 동적 분리가 실제로 동작하지 않는 것을 확인하여 이 방식으로 변경 — 이후 CORS 처리 위치 자체가 `SecurityConfig`로 다시 이동했으며, 자세한 내용은 아래 9) 인증 & 보안 참고)
- 목록 조회는 `type`, `from`, `to`, `Pageable` 파라미터를 받아 검색·페이징 처리
- 수정은 `PUT /workouts/{id}`로 처리 — 리소스 전체 교체
- 통계 API(`stats/by-type`, `stats/monthly`)도 `from`/`to` 기간 필터 지원 (미지정 시 전체 집계)
- `@Tag`, `@Operation`, `@Parameter`, `@ApiResponses`로 Swagger 문서 자동 생성 (springdoc-openapi)
- `WorkoutController`의 모든 엔드포인트가 `@AuthenticationPrincipal Long userId`로 인증된 사용자 id를 받아 소유자 기준으로 동작 — 다른 사용자의 기록을 조회/수정/삭제하려 하면 403이 아니라 `WorkoutNotFoundException`(404)으로 응답해, 기록이 "존재하지만 권한이 없다"는 사실 자체를 노출하지 않음
- `AuthController`(`/auth/**`)는 인증 없이 접근 가능 — 회원가입(`/signup`), 로그인(`/login`), Google 로그인(`/google`) 3개 엔드포인트만 노출

### 2) Domain
**Workout / RunningWorkout / BoxingWorkout**
- `Workout`은 `abstract` 클래스, `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` + `@DiscriminatorColumn(name = "dtype")` 적용
- `RunningWorkout`(distanceKm, place, caloriesBurned), `BoxingWorkout`(rounds, sparringPartner, techniqueType)이 `Workout`을 상속
- 정적 팩토리 메서드 `create()`로만 생성 — 객체 생성 방식 통제 (생성자는 private)
- `update()`로 같은 타입 내 필드 전체 교체 — 타입 자체는 변경 불가(요청 타입이 기존과 다르면 `WorkoutTypeMismatchException`)
- `addDetail()` / `clearDetails()`로 WorkoutDetail 추가·전체 교체 — 연관관계 편의 메서드
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — JPA 프록시 생성용, 외부 직접 호출 차단
- `@CreatedDate`/`@LastModifiedDate` + `@EntityListeners(AuditingEntityListener.class)`로 생성/수정 시각 자동 관리 (`AuditingConfig`의 `@EnableJpaAuditing`으로 활성화)
- id는 `@GeneratedValue(strategy = GenerationType.IDENTITY)`로 DB가 채번 (별도 id 할당기는 없음)
- `Workout`이 `@ManyToOne(fetch = LAZY) User owner`(컬럼 `user_id`, `nullable = false`)를 가짐 — 모든 운동 기록은 반드시 특정 회원 한 명에게 귀속되며, 생성 시점부터 owner 없이는 저장 불가

**WorkoutDetail**
- `@ManyToOne(fetch = FetchType.LAZY)` — 지연 로딩으로 불필요한 쿼리 방지

**User**
- `email`(unique, not null) / `password`(nullable — OAuth 전용 계정은 비밀번호 없음) / `role`(`UserRole`) / `provider`(`AuthProvider`) / `providerId`
- `(provider, provider_id)` 복합 유니크 제약(`uk_users_provider_provider_id`)
- 정적 팩토리 `create()`(로컬 이메일 가입, 비밀번호 필수) / `createOAuth()`(OAuth 가입, 비밀번호 없이 provider/providerId만) — 생성자 private, `hasPassword()`로 로컬/OAuth 계정 구분
- `UserRole`(USER, ADMIN)은 현재 필드만 존재 — 실제 권한 분기(RBAC) 로직은 아직 없고, `JwtAuthenticationFilter`가 만드는 `Authentication`에도 권한(`GrantedAuthority`)이 비어 있음(`List.of()`)
- `AuthProvider`(LOCAL, GOOGLE, KAKAO, NAVER) 중 실제로 로그인 로직이 구현된 건 GOOGLE뿐 — KAKAO/NAVER는 enum 값만 정의돼 있고 연동 코드는 없음

### 3) DTO
- `record` 타입으로 불변 객체 — 요청 데이터 변경 불필요
- Bean Validation으로 입력값 검증 (`@NotNull`, `@Min`, `@Max`, `@Size`, `@PastOrPresent`, `@Positive`)
- `WorkoutForm`에 러닝·복싱 전용 필드를 모두 두고, `@AssertTrue` 커스텀 검증 4종으로 타입-필드 정합성을 양방향 체크
  - 러닝인데 `distanceKm` 없으면 400 / 복싱인데 `rounds` 없으면 400
  - 복싱인데 러닝 전용 필드(`distanceKm`/`place`/`caloriesBurned`)를 보내도 400 / 러닝인데 복싱 전용 필드(`rounds`/`sparringPartner`/`techniqueType`)를 보내도 400
  - 등록/수정 폼이 필드·검증 로직이 100% 동일해 `WorkoutForm` 하나로 통합, 타입 불변성(수정 시 타입 변경 금지)은 서비스 계층에서 처리
- `WorkoutResponse`/`WorkoutSummaryResponse`는 `WorkoutTypeFields`로 `instanceof` 분기를 공통화해 타입별 필드를 노출
- `WorkoutSummaryResponse` — 목록 조회 전용 DTO, `details` 제외
  (컬렉션 fetch join + 페이징 조합 시 발생하는 HHH000104 경고를 원천 차단)
- `PageResponse<T>` — Spring `Page`를 그대로 직렬화하지 않고 `content`/`number`/`size`/`totalElements`/`totalPages`/`first`/`last`만 감싼 응답
- `WorkoutStatByType`, `WorkoutMonthlyStat` — 통계 API용 DTO Projection
- `@Schema`로 필드 설명·예시 값 부여 (Swagger 문서화)
- `SignupRequest`(`@Email`, 비밀번호 8~64자) / `LoginRequest`(`@Email` + 비밀번호) / `GoogleLoginRequest`(Google ID 토큰 문자열 하나) — 인증 요청 DTO
- `TokenResponse` — 정적 팩토리 `of(accessToken)`으로 `accessToken` + 고정 `tokenType="Bearer"`를 함께 반환, 응답 포맷 통일

### 4) Repository
- 인터페이스 분리 — `WorkoutRepository`(CRUD 전용: save/findById/delete)와
  `WorkoutQueryRepository`(검색/통계: search/statsByType/statsByMonth)를 분리
  - 서비스는 두 인터페이스에만 의존하는 구조라 구현체 교체가 가능하지만, 현재 구현체는 `JpaWorkoutRepositoryAdapter` 하나뿐입니다(`@Primary`는 지금 시점에는 별 의미 없이 붙어 있는 상태).
- `JpaWorkoutRepository` — Spring Data JPA, `JpaSpecificationExecutor` 상속
  - 단건 조회(`findByIdWithDetails`)는 `LEFT JOIN FETCH`로 N+1 해결
  - 목록 검색은 `WorkoutSpecifications`로 동적 조건을 조합해 처리 (파생 쿼리 메서드 방식 대신 Specification 사용)
  - 통계 쿼리는 `from`/`to` 널 허용 조건(`:from is null or ...`)으로 기간 필터를 옵션 처리
- `JpaWorkoutRepositoryAdapter` — `WorkoutRepository`, `WorkoutQueryRepository` 모두 구현
  - `search()`는 type/from/to 존재 여부에 따라 `WorkoutSpecifications`의 조건들을 동적으로 조합
  - `from`/`to` 경계는 둘 다 포함(`greaterThanOrEqualTo`/`lessThanOrEqualTo`)으로 통일
  - `distanceKm` 정렬은 `cb.treat()`로 서브클래스(RunningWorkout) 필드에 접근, `CASE WHEN` 표현식으로 NULLS LAST 처리 (다른 타입 레코드는 정렬 결과 뒤로)
- 통계는 DTO Projection(`select new ...`)으로 DB GROUP BY 집계 — 애플리케이션 레벨 합산 금지
  - `statsByType()`은 `coalesce(sum(...), 0L)`로 합계가 null이 되는 경우를 방어
  - `statsByMonth()`은 `extract(year/month from ...)`를 사용해 DB 방언 종속성 제거
- `WorkoutQueryRepository`의 `search`/`statsByType`/`statsByMonth` 시그니처는 `User` 엔티티가 아니라 `Long ownerId`를 첫 파라미터로 받음 — JPQL에서도 `w.owner = :owner`가 아니라 `w.owner.id = :ownerId`(경로 표현식)로 비교. 최종적으로는 같은 FK 컬럼 비교로 처리되지만, `User owner`를 받는 시그니처는 호출부가 `User` 엔티티 전체를 미리 로드해서 넘겨야 한다는 제약을 만드는 반면, `Long ownerId`는 인증된 userId만 있으면 되고 `User`를 조회할 필요가 없음. 회원별 격리는 그대로 서비스 레이어 후처리가 아니라 JPA 쿼리 자체(`WHERE w.owner.id = :ownerId`)에서 처리
- `UserRepository`(`findByEmail`, `existsByEmail`)로 가입 시 이메일 중복 체크, 로그인 시 계정 조회, Google 로그인 시 이메일 기준 기존 계정 자동 연결을 모두 처리

### 5) Service
- `@Transactional(readOnly = true)` 클래스 레벨 적용 — 조회 성능 최적화
- 쓰기 메서드에만 `@Transactional` 재선언
- 목록 조회 시 정렬 필드 화이트리스트 검증 (`workoutDateTime`, `durationMinutes`, `type`, `distanceKm`만 허용)
- 페이지 크기 상한은 `spring.data.web.pageable.max-page-size`(100) 설정으로 초과분을 자동 조정
- `update()`는 기존 엔티티를 조회해 같은 타입인지 확인 후 필드 갱신, details는 전체 삭제 후 재생성
- `WorkoutService`의 쓰기 경로(`save()`)만 `resolveOwner()`(`userRepository.findById()`)로 `User` 엔티티 전체를 조회해서 사용 — 없으면 `AuthenticatedUserNotFoundException`(401). 조회 경로(`search()`/`statsByType()`/`statsByMonth()`)는 `User`를 로드할 필요가 없으므로 `validateUserExists()`(`existsById()`)로 존재 여부만 가볍게 확인한 뒤 `userId`(`Long`)를 그대로 쿼리에 넘김. 단건 조회·수정·삭제는 먼저 `validateOwner()`로 소유자 일치 여부를 확인(불일치 시 `WorkoutNotFoundException`)
- `AuthService.signup()` — 이메일 중복이면 `EmailAlreadyExistsException`(409), 아니면 `BCryptPasswordEncoder`로 비밀번호를 해싱해 `User.create()`로 저장. `existsByEmail()` 체크와 저장 사이의 시간차로 동시에 같은 이메일이 가입 요청되면 둘 다 통과할 수 있는데, 저장을 `saveAndFlush()`로 즉시 반영해 DB 유니크 제약(`email`) 위반을 `DataIntegrityViolationException`으로 받아 다시 `EmailAlreadyExistsException`(409)으로 변환 — 애플리케이션 레벨 체크가 뚫려도 DB 제약이 최후 방어선 역할
- `AuthService.login()` — `hasPassword()`로 OAuth 전용 계정(비밀번호 없음)이 일반 로그인을 시도하는 경우를 방어, 이메일/비밀번호 불일치는 계정 존재 여부를 구분하지 않고 동일하게 `InvalidCredentialsException`(401). 메시지뿐 아니라 응답 시간도 통일 — 계정이 없거나 비교할 비밀번호가 없는 경우에도 고정된 더미 BCrypt 해시로 `matches()`를 동일하게 한 번 호출시켜 "계정이 있고 비밀번호만 틀린 경우"와 처리 시간을 맞춤(타이밍 사이드채널 방어)
- `AuthService.loginWithGoogle()` — `GoogleIdTokenValidator`로 ID 토큰을 검증하고, `email_verified`가 아니면 거부. 이메일이 같으면 기존 계정(로컬 가입이든 이미 Google 연동이든 무관)에 자동 로그인 처리, 없으면 신규 OAuth 계정 자동 생성. 클래스 레벨 `readOnly` 트랜잭션과 별개로 이 조회+저장 구간만 `TransactionTemplate`으로 감싸 쓰기 트랜잭션 경계를 명시적으로 둠
  - 다만 `findByEmail().orElseGet(() -> save(...))` 자체가 원자적인 건 아님 — 같은 신규 이메일로 동시에 Google 로그인이 여러 번 들어오면 두 트랜잭션이 각각 `findByEmail`을 통과한 뒤 각각 `save()`를 시도해 `email` unique 제약 위반이 이론적으로는 여전히 가능. `signup()`과 달리 이 경로에는 `DataIntegrityViolationException`을 잡아 재조회하는 방어 코드가 아직 없음(보완 필요 항목)

### 6) Exception
- `@RestControllerAdvice`로 전역 예외 처리
- `WorkoutNotFoundException` → 404
- `MethodArgumentNotValidException` → 400 + 필드별 에러 메시지
- `MethodArgumentTypeMismatchException` → 400 (요청 파라미터 타입 오류)
- `InvalidSortPropertyException` → 400 (화이트리스트에 없는 정렬 필드 요청)
- `WorkoutTypeMismatchException` → 400 (수정 시 타입 변경 시도)
- `EmailAlreadyExistsException` → 409 (회원가입 시 이미 가입된 이메일)
- `InvalidCredentialsException` → 401 (이메일/비밀번호 불일치 — 파라미터 없는 고정 메시지로 계정 존재 여부 비노출)
- `InvalidGoogleTokenException` → 401 (Google ID 토큰 검증 실패 중 토큰 형식/서명/`email_verified` 문제 — 클라이언트가 보낸 값 자체의 문제)
- `GoogleVerificationUnavailableException` → 503 (Google 공개키 서버 조회 등 인프라/네트워크 문제로 서명 검증 자체를 끝내지 못한 경우 — 토큰이 잘못된 게 아니라 서버가 검증을 못 끝낸 것이므로 401이 아니라 5xx로 분리, 원인 추적을 위해 서버 로그도 남김)
- `AuthenticatedUserNotFoundException` → 401 (JWT는 유효하지만 그 토큰의 `sub`(userId)에 해당하는 사용자가 DB에 없는 경우 — 탈퇴 등으로 유저가 사라진 뒤에도 유효기간이 안 지난 토큰을 들고 오는 케이스)
- `DataIntegrityViolationException` 전용 핸들러는 아직 없음 — `AuthService.signup()`은 로컬에서 직접 캐치해 `EmailAlreadyExistsException`(409)으로 변환하지만, `AuthService.loginWithGoogle()`의 동시 신규 가입 경합(위 5) 참고)은 이 방어가 아직 없어 실제로 발생하면 그대로 500으로 노출됨(보완 필요 항목)

### 7) API 문서화 (Swagger)
- `springdoc-openapi-starter-webmvc-ui`로 OpenAPI 3.1 스펙 자동 생성
- `@Tag`/`@Operation`/`@Parameter`로 엔드포인트 설명, `@Schema`로 DTO 필드 설명·예시 값 부여
- 400/404 에러 응답을 `@ApiResponses` + `ErrorResponse` 스키마로 연결
- Bearer 인증용 SecurityScheme은 아직 등록돼 있지 않아, 인증이 필요한 API를 Swagger UI에서 직접 실행(Try it out)하면 401이 남 — 토큰을 헤더에 넣을 방법이 UI상에 없음

### 8) CI (GitHub Actions)
- PR 생성·main push 시 `./gradlew build` 자동 실행 (JDK 17, Testcontainers MySQL 포함)
- main 브랜치 보호 규칙 적용 — CI(`build`) 통과 없이는 병합 불가

### 9) 인증 & 보안
- Spring Security를 stateless로 구성 — `SessionCreationPolicy.STATELESS`, `csrf` 비활성화(토큰 기반 인증이라 서버가 세션/쿠키 상태를 들고 있지 않음), `anonymous(AbstractHttpConfigurer::disable)`로 익명 인증도 꺼둠
- Security 필터체인은 두 개로 분리 — `/h2-console/**`은 별도의 `@Order(1)` 체인(`h2ConsoleFilterChain`)에서 `sameOrigin()` 프레임 옵션과 함께 전부 permitAll, 나머지 전체 요청을 다루는 `@Order(2)` 메인 체인(`filterChain`)의 `authorizeHttpRequests`에서는 `OPTIONS /**`와 `/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`만 permitAll이고 나머지(`/workouts/**` 포함)는 전부 인증 필요
  - `OPTIONS`를 별도로 최우선 순위에 permitAll 해둔 이유: 브라우저가 보내는 인증 없는 preflight 요청이 `AuthorizationFilter`에 먼저 막히는 걸 방지하기 위한 안전장치
- `JwtAuthenticationFilter`(`OncePerRequestFilter`)가 `Authorization: Bearer <token>` 헤더를 파싱해 토큰이 유효하면 `SecurityContext`에 인증 정보를 세팅. 별도 `UserDetailsService` 없이 principal 자체가 토큰의 `sub`(userId, `Long`)이고 `@AuthenticationPrincipal Long userId`로 컨트롤러에서 바로 받아 씀. 토큰이 무효하면 그 사유(`TokenStatus.EXPIRED`/`INVALID`)를 요청 attribute에 남겨 인증 실패 처리 단계로 넘김
- 인증 실패 시 `CustomAuthenticationEntryPoint`가 그 attribute를 읽어 `TOKEN_EXPIRED`/`TOKEN_INVALID`/`TOKEN_MISSING`(attribute 자체가 없으면 토큰 미제출로 간주) 중 하나의 `errorCode`와 함께 구조화된 401 JSON을 응답 — `ErrorResponse`에 `errorCode` 필드가 추가됐고, 기존 3-인자 생성자는 호환용으로 유지됨. 프론트가 "만료라 재로그인이 필요한지" vs "토큰 자체가 잘못됐는지"를 문자열 매칭 없이 `errorCode`로 구분 가능
- `JwtTokenProvider`가 JJWT 0.12.6으로 HMAC-SHA 서명 토큰을 발급(`sub=userId`, `email` 클레임 포함), 만료는 `jwt.access-token-expiration-ms`(기본 1시간) — Refresh Token은 아직 없어서 Access Token 만료 후에는 재로그인이 필요함
- 비밀번호는 `BCryptPasswordEncoder`로 해싱해서 저장, 평문 비교 코드 없음
- Google 로그인은 `GoogleIdTokenValidator`(`GoogleIdTokenVerifier`, google-api-client)가 서버 쪽에서 직접 ID 토큰의 서명/`aud`(=`google.client-id`)를 검증 — 프론트가 보낸 값을 그대로 신뢰하지 않음. `email_verified=false`인 계정은 거부. 토큰 형식 자체가 잘못된 경우(`IllegalArgumentException`)는 401로, 서명 검증 과정(구글 공개키 서버 조회 등)에서 나는 인프라/네트워크 문제(`GeneralSecurityException`/`IOException`)는 클라이언트 잘못이 아니므로 503으로 구분해 응답
- CORS는 처음엔 `WebConfig`(MVC 레벨)에서 처리했지만, 지금은 `SecurityConfig.corsConfigurationSource()`로 완전히 옮겨서 `HttpSecurity.cors(cors -> cors.configurationSource(...))`로 명시적으로 연결돼 있음 — `WebConfig`는 그 사실을 남겨두는 주석만 있는 빈 설정 클래스로 남음. 옮긴 이유는 Security 필터체인이 MVC보다 먼저 요청을 가로채기 때문에, 인증 실패 응답(401)에도 CORS 헤더가 붙으려면 CORS 처리가 MVC가 아니라 Security 레벨에서 이뤄져야 하기 때문(MVC 레벨 CORS는 컨트롤러까지 요청이 도달해야 붙는데, 인증 실패는 컨트롤러 도달 전에 걸림). `OPTIONS` permitAll은 이 이후에도 안전장치로 그대로 남겨둠

---

## 📡 API 엔드포인트
| Method | URI | 설명 |
|--------|-----|------|
| POST | `/auth/signup` | 이메일/비밀번호 회원가입 → 201 |
| POST | `/auth/login` | 이메일/비밀번호 로그인 → `TokenResponse`(accessToken) |
| POST | `/auth/google` | Google ID 토큰으로 로그인 (없으면 자동 가입) → `TokenResponse` |
| GET | `/workouts` | 운동 기록 검색 (type, from, to, page, size, sort) → `PageResponse` (인증 필요, 본인 기록만) |
| POST | `/workouts` | 운동 기록 등록 → 201 + `Location` 헤더 (인증 필요) |
| GET | `/workouts/{id}` | 운동 기록 단건 조회 (details 포함, 인증 필요, 타인 기록이면 404) |
| PUT | `/workouts/{id}` | 운동 기록 수정 (타입 변경 불가, 인증 필요, 타인 기록이면 404) |
| DELETE | `/workouts/{id}` | 운동 기록 삭제 (인증 필요, 타인 기록이면 404) |
| GET | `/workouts/stats/by-type` | 타입별 통계 (count, 총 운동시간), from/to 기간 필터 가능 (인증 필요, 본인 기록만) |
| GET | `/workouts/stats/monthly` | 월별 운동 횟수 통계, from/to 기간 필터 가능 (인증 필요, 본인 기록만) |

`/auth/**`를 제외한 모든 요청은 `Authorization: Bearer <accessToken>` 헤더가 필요합니다.

예시:

    POST /auth/login
    { "email": "user@example.com", "password": "password123" }

    GET /workouts?type=BOXING&from=2026-05-01T00:00:00&to=2026-05-31T23:59:59&sort=distanceKm,desc&page=0&size=10
    Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

    GET /workouts/stats/by-type?from=2026-01-01T00:00:00&to=2026-06-30T23:59:59
    Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

전체 스펙은 Swagger UI(`/swagger-ui/index.html`) 참고. (단, 위에서 언급했듯 인증 필요한 API는 Swagger에서 바로 실행은 안 됨)

---

## ✅ 구현 기능
- 운동 기록 등록 / 단건 조회 / 수정 / 삭제 API
- 러닝/복싱 타입별 속성 분화 (JPA SINGLE_TABLE 상속) 및 타입-필드 정합성 검증(양방향)
- 운동 세부 기록 (WorkoutDetail) 1:N 관계 관리, 수정 시 전체 교체
- 서버 사이드 유효성 검증
- 단건 조회 N+1 문제 해결 (LEFT JOIN FETCH)
- 전역 예외 처리
- CRUD/Query 인터페이스 분리로 구현체 교체가 가능한 구조 (현재 구현체는 JPA 어댑터 하나)
- CORS 설정으로 프론트엔드 연동 (설정 파일로 externalize, Security 레벨에서 처리)
- 타입·기간 조건 검색 + 정렬 (Specification 기반, 경계 정책 통일), 타입 전용 필드 정렬 시 NULLS LAST 처리
- 정렬 필드 화이트리스트 검증
- 페이징 처리 (`PageResponse`로 감싼 응답, 목록 조회에서 details 제외, 상한 100건 자동 조정)
- DB 집계 기반 통계 API (타입별 / 월별), 기간 필터(from/to), null 합계 방어 및 DB 방언 이식성 확보
- JPA Auditing (createdAt, updatedAt 자동 관리)
- Swagger(OpenAPI) API 문서 자동 생성
- GitHub Actions CI + main 브랜치 보호
- 이메일/비밀번호 회원가입·로그인 (BCrypt 해싱, 이메일 중복 가입 방지 — 애플리케이션 체크 + DB 유니크 제약 이중 방어, 401/409 구분 응답, 로그인 실패 시 타이밍 사이드채널 방어)
- Google OAuth2 로그인 (ID 토큰 서버 검증, `email_verified` 확인, 토큰 형식 문제(401)와 검증 인프라 문제(503) 구분, 이메일 기준 기존 계정 자동 연결 또는 신규 자동 가입)
- Stateless JWT 인증 (Access Token 발급/검증, `Authorization: Bearer` 헤더 기반, 세션 미사용, 만료/무효/미제출을 구분하는 구조화된 401 에러코드)
- 운동 기록 소유자(User) 기반 접근 제어 — 회원별 데이터 격리, 타인 기록은 존재 여부 자체를 노출하지 않고 404로 응답
- 탈퇴 등으로 사라진 사용자의 유효 토큰에 대한 방어(`AuthenticatedUserNotFoundException` → 401)
- 테스트 실행이 셸 환경변수(`JWT_SECRET` 등)에 의존하지 않도록 테스트 전용 설정 분리

---

## 향후 확장 계획 (기획 중)
현재 완성된 CRUD/검색/통계/인증 서버를 베이스로 아래 기능을 검토 중. 기술적으로 난이도 차이가 커서 우선순위를 나눔.

| 기능 | 핵심 기술 | 난이도 |
|------|-----------|--------|
| Refresh Token / 로그아웃 | Refresh Token 발급·로테이션, 토큰 무효화(블랙리스트 or DB 저장) | 중 |
| Kakao / Naver 로그인 | Spring Security OAuth2, 카카오·네이버 API 연동 (AuthProvider enum에 값은 이미 정의돼 있으나 연동 로직은 없음) | 중 |
| 회원별 관리 (프로필, RBAC) | Spring Security 권한 체계(현재 UserRole 필드만 존재), 프로필(닉네임/이미지) 저장, S3 연동 | 중 |
| 챌린지 & 배지 시스템 | Redis Sorted Set, 이벤트 기반 처리 | 상 |
| 오운완 기록 공유 | Web Share API (모바일 브라우저 네이티브 공유 시트) — 기획 초안의 Instagram Graph API/Stories 연동은 개인 계정 스토리 공유 용도로는 부적합해 웹 표준 API로 방향 수정 |

전체 기획안 및 마일스톤은 별도 문서 참고
