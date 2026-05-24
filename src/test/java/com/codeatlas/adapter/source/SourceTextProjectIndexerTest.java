package com.codeatlas.adapter.source;

import com.codeatlas.core.project.ImplementationDescriptor;
import com.codeatlas.core.project.JavaMethodDescriptor;
import com.codeatlas.core.project.JavaTypeDescriptor;
import com.codeatlas.core.project.ProjectIndex;
import com.codeatlas.core.project.SpringBeanDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceTextProjectIndexerTest {
    @TempDir
    Path tempDir;

    @Test
    void indexesSimpleJavaClassAndMethodsFromMainJava() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/company/FooService.java"),
                """
                        package com.company;

                        public class FooService {
                            public void processOrder(String orderId) {
                            }
                        }
                        """
        );

        ProjectIndex index = new SourceTextProjectIndexer().index(tempDir);

        assertEquals("1.0", index.schemaVersion());
        assertEquals("Java", index.language());
        assertEquals("src/main/java", index.sourceRoots().get(0));
        assertEquals(1, index.classes().size());

        JavaTypeDescriptor fooService = index.classes().get(0);
        assertEquals("com.company.FooService", fooService.fullyQualifiedName());
        assertEquals("com.company", fooService.packageName());
        assertEquals("FooService", fooService.simpleName());
        assertEquals("CLASS", fooService.kind());
        assertEquals("src/main/java/com/company/FooService.java", fooService.sourceFile());
        assertEquals(1, fooService.methods().size());

        JavaMethodDescriptor method = fooService.methods().get(0);
        assertEquals("processOrder", method.name());
        assertEquals("processOrder(String)", method.signature());
        assertEquals("PUBLIC", method.visibility());
        assertEquals("void", method.returnType());
        assertEquals("src/main/java/com/company/FooService.java", method.sourceLocation().file());
    }

    @Test
    void indexesInterfacesAndConcreteImplementations() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/company/UserRegistrationInternal.java"),
                """
                        package com.company;

                        public interface UserRegistrationInternal {
                            void register();
                        }
                        """
        );
        writeJavaFile(
                tempDir.resolve("src/main/java/com/company/UserServiceImpl.java"),
                """
                        package com.company;

                        public class UserServiceImpl implements UserRegistrationInternal {
                            public void register() {
                            }
                        }
                        """
        );

        ProjectIndex index = new SourceTextProjectIndexer().index(tempDir);

        assertEquals(1, index.interfaces().size());
        assertEquals("com.company.UserRegistrationInternal", index.interfaces().get(0).fullyQualifiedName());

        ImplementationDescriptor implementation = index.implementations().get(0);
        assertEquals("com.company.UserRegistrationInternal", implementation.interfaceName());
        assertEquals(1, implementation.implementations().size());
        assertEquals("com.company.UserServiceImpl", implementation.implementations().get(0));
    }

    @Test
    void indexesSpringBeansRepositoriesClientsAndHttpEntrypoints() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/company/AuthController.java"),
                """
                        package com.company;

                        import org.springframework.web.bind.annotation.PostMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/auth")
                        public class AuthController {
                            @PostMapping("/register")
                            public void register() {
                            }
                        }
                        """
        );
        writeJavaFile(
                tempDir.resolve("src/main/java/com/company/UserServiceImpl.java"),
                """
                        package com.company;

                        import org.springframework.stereotype.Service;

                        @Service
                        public class UserServiceImpl {
                            public void create() {
                            }
                        }
                        """
        );
        writeJavaFile(
                tempDir.resolve("src/main/java/com/company/UserRepository.java"),
                """
                        package com.company;

                        public interface UserRepository extends JpaRepository<User, Long> {
                        }
                        """
        );
        writeJavaFile(
                tempDir.resolve("src/main/java/com/company/TransferServiceClient.java"),
                """
                        package com.company;

                        public interface TransferServiceClient {
                            void send();
                        }
                        """
        );

        ProjectIndex index = new SourceTextProjectIndexer().index(tempDir);

        assertEquals("Spring", index.frameworks().get(0));
        assertTrue(index.controllers().contains("com.company.AuthController"));
        assertTrue(index.repositories().contains("com.company.UserRepository"));
        assertTrue(index.clients().contains("com.company.TransferServiceClient"));

        SpringBeanDescriptor controllerBean = index.springBeans().stream()
                .filter(bean -> bean.beanType().equals("com.company.AuthController"))
                .findFirst()
                .orElseThrow();
        assertEquals("authController", controllerBean.id());
        assertEquals("REST_CONTROLLER", controllerBean.kind());
        assertTrue(controllerBean.annotations().contains("RestController"));

        SpringBeanDescriptor serviceBean = index.springBeans().stream()
                .filter(bean -> bean.beanType().equals("com.company.UserServiceImpl"))
                .findFirst()
                .orElseThrow();
        assertEquals("userServiceImpl", serviceBean.id());
        assertEquals("SERVICE", serviceBean.kind());

        assertEquals(1, index.entrypoints().size());
        assertEquals("POST", index.entrypoints().get(0).httpMethod());
        assertEquals("/auth/register", index.entrypoints().get(0).path());
        assertEquals("com.company.AuthController.register", index.entrypoints().get(0).javaEntrypoint());
    }

    private static void writeJavaFile(Path file, String source) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }
}
