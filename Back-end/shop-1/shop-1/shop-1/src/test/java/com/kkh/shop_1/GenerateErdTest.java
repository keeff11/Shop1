package com.kkh.shop_1;

import com.kkh.shop_1.common.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(QuerydslConfig.class)
public class GenerateErdTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void generateMermaidErdAndInjectToReadme() {
        try {
            System.out.println("========== ERD 자동 생성 프로세스 시작 ==========");

            StringBuilder sb = new StringBuilder();
            sb.append("```mermaid\n");
            sb.append("erDiagram\n");

            Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();

            // 1. 엔티티 및 컬럼 파싱 (Null-Safe 방어 코드 적용)
            for (EntityType<?> entity : entities) {
                if (entity.getJavaType() == null) continue;
                String tableName = entity.getJavaType().getSimpleName();
                sb.append("    ").append(tableName).append(" {\n");

                for (Attribute<?, ?> attribute : entity.getAttributes()) {
                    if (!attribute.isAssociation()) {
                        String type = (attribute.getJavaType() != null)
                                ? attribute.getJavaType().getSimpleName().replace("[]", "")
                                : "Unknown";
                        String name = (attribute.getName() != null) ? attribute.getName() : "unknown";
                        sb.append("        ").append(type).append(" ").append(name).append("\n");
                    }
                }
                sb.append("    }\n\n");
            }

            // 2. 연관관계 파싱 (단방향 최적화 및 Null-Safe 방어 코드 적용)
            for (EntityType<?> entity : entities) {
                if (entity.getJavaType() == null) continue;
                String sourceTable = entity.getJavaType().getSimpleName();

                for (Attribute<?, ?> attribute : entity.getAttributes()) {
                    if (attribute.isAssociation() && !(attribute instanceof PluralAttribute)) {
                        if (attribute.getJavaType() != null) {
                            String targetTable = attribute.getJavaType().getSimpleName();
                            sb.append("    ").append(sourceTable).append(" }o--|| ").append(targetTable).append(" : references\n");
                        }
                    }
                }
            }
            sb.append("```\n");

            System.out.println("========== ERD 텍스트 파싱 완료. README 주입 시작 ==========");
            updateReadmeWithErd(sb.toString());
            System.out.println("========== README 주입 성공! ==========");

        } catch (Exception e) {
            // 💡 GitHub Actions 로그에 정확한 에러 원인을 강제로 출력시킵니다.
            System.err.println("🚨 ERD 생성 중 치명적 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("ERD 생성 실패", e);
        }
    }

    private void updateReadmeWithErd(String mermaidCode) throws IOException {
        Path readmePath = Paths.get("../../../../README.md");
        if (!Files.exists(readmePath)) {
            System.out.println("⚠️ README.md 파일을 찾을 수 없습니다: " + readmePath.toAbsolutePath());
            return; // 파일이 없으면 에러를 내지 않고 안전하게 종료합니다.
        }

        List<String> lines = Files.readAllLines(readmePath, StandardCharsets.UTF_8);
        StringBuilder newContent = new StringBuilder();
        boolean inErdBlock = false;

        for (String line : lines) {
            if (line.contains("")) {
                newContent.append(line).append("\n");
                newContent.append(mermaidCode);
                inErdBlock = true;
                continue;
            }
            if (line.contains("")) {
                inErdBlock = false;
            }
            if (!inErdBlock) {
                newContent.append(line).append("\n");
            }
        }

        Files.write(readmePath, newContent.toString().getBytes(StandardCharsets.UTF_8));
    }
}