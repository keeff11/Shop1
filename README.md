# 🛒 Shop1 - 대규모 트래픽을 고려한 이커머스 플랫폼

**Shop1**은 실제 상용 서비스를 목표로 개발된 풀스택 이커머스 플랫폼입니다.  
상품 관리, 주문/결제, 배송 시스템을 마이크로서비스 아키텍처(MSA)를 고려한 모듈형 모놀리식 구조로 설계하였으며, Docker와 CI/CD를 통한 자동화된 배포 파이프라인을 구축했습니다.

🔗 **배포 링크:** [http://shop1.cloud](http://shop1.cloud)  
📚 **상세 포트폴리오(Notion):** [Notion](https://quasar-carnation-faa.notion.site/8b6636b4e262432bae9359e4948b8826?pvs=74)

---

## 🛠 Tech Stack

### Backend
* **Java 17, Spring Boot 3.x**: 최신 LTS 버전을 활용한 안정적인 서버 구축
* **JPA (Hibernate), QueryDSL**: 복잡한 동적 쿼리 처리 및 타입 안전성 확보
* **Redis**: 캐싱을 통한 조회 성능 개선 및 분산 환경 세션 관리
* **MySQL 8.0**: 대용량 데이터 저장을 위한 관계형 데이터베이스

### Frontend & Mobile
* **Next.js (React), TypeScript**: SSR 기반의 SEO 최적화 및 빠른 렌더링
* **Flutter**: 크로스 플랫폼 모바일 앱 지원

### Infra & DevOps
* **Docker, Docker Compose**: 개발 및 프로덕션 환경의 일치화
* **GitHub Actions**: 테스트 및 배포 자동화 (CI/CD)
* **AWS EC2**: 클라우드 서버 호스팅

---

## 🚀 Key Achievements & Troubleshooting

이 프로젝트를 진행하며 겪은 주요 기술적 도전과 해결 사례입니다. 상세 내용은 `docs/` 폴더를 참고해주세요.

1.  **Docker 배포 시 데이터 초기화 및 영속성 관리 전략 개선**
    * **문제:** CI/CD 배포 시 기존 데이터가 남아있어 초기화 로직(`DataInitializer`)이 동작하지 않거나, 반대로 원치 않게 데이터가 유실되는 문제 발생.
    * **해결:** Docker Volume의 생명주기를 이해하고, `ddl-auto` 전략과 연계하여 배포 스크립트(`docker compose down -v`)를 상황에 맞게 최적화함.
    * 👉 [자세히 보기 (docs/TROUBLESHOOTING.md)](./docs/TROUBLESHOOTING.md)

2.  **실무 환경과 유사한 대규모 더미 데이터 구축**
    * **문제:** 단순 랜덤 데이터(Faker)로는 실제 서비스의 UI/UX 테스트에 한계가 있음.
    * **해결:** JSON 기반의 고품질 데이터셋을 구축하고, 애플리케이션 구동 시점에 데이터를 증폭(Amplify)하여 주입하는 로직 구현.

---

## 🏃 How to Run

이 프로젝트는 Docker Compose를 통해 한 번의 명령어로 실행할 수 있습니다.

```bash
# 1. 저장소 클론
git clone [https://github.com/keeff11/shop1.git](https://github.com/keeff11/shop1.git)
cd shop1

# 2. 환경 변수 설정 (.env 파일 생성 필요)
# (비밀 키 등은 제외되어 있습니다.)

# 3. 빌드 및 실행 (캐시 없이 재빌드)
docker compose up -d --build --force-recreate