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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

            // 1. 엔티티 파싱
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

            // 2. 연관관계 파싱
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
            
            File readmeFile = new File("../../../../README.md");
            if (!readmeFile.exists()) {
                System.out.println("⚠️ README.md 파일을 찾을 수 없습니다: " + readmeFile.getAbsolutePath());
                return;
            }

            String content = new String(Files.readAllBytes(readmeFile.toPath()), StandardCharsets.UTF_8);

            String startMarker = "";
            String endMarker = "";

            int startIndex = content.indexOf(startMarker);
            int endIndex = content.indexOf(endMarker);

            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                String before = content.substring(0, startIndex + startMarker.length());
                String after = content.substring(endIndex);

                String newContent = before + "\n" + sb.toString() + after;

                // 덮어쓰기 저장
                Files.write(readmeFile.toPath(), newContent.getBytes(StandardCharsets.UTF_8));
                System.out.println("========== README 주입 성공! ==========");
            } else {
                System.out.println("⚠️ README.md 안에 마커 주석()이 올바르게 존재하지 않습니다.");
            }

        } catch (Throwable t) {
            // Error, Exception 등 어떤 문제가 생겨도 무조건 화면에 출력
            System.out.println("🚨 ERD 생성 중 치명적 오류 발생: " + t.getMessage());
            t.printStackTrace(System.out);
            throw new RuntimeException("ERD 생성 실패", t);
        }
    }
}