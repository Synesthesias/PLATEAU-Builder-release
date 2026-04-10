package org.plateaubuilder.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import org.plateaubuilder.core.io.gml.GmlImporter;
import javafx.scene.Group;
import org.plateaubuilder.core.world.World;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.plateaubuilder.validation.constant.StandardID.C01;
import static org.plateaubuilder.validation.constant.StandardID.C04;
import static org.plateaubuilder.validation.constant.StandardID.C_BLDG_01;
import static org.plateaubuilder.validation.constant.StandardID.L04;
import static org.plateaubuilder.validation.constant.StandardID.L05;
import static org.plateaubuilder.validation.constant.StandardID.L06;
import static org.plateaubuilder.validation.constant.StandardID.L07;
import static org.plateaubuilder.validation.constant.StandardID.L08;
import static org.plateaubuilder.validation.constant.StandardID.L09;
import static org.plateaubuilder.validation.constant.StandardID.L10;
import static org.plateaubuilder.validation.constant.StandardID.L11;
import static org.plateaubuilder.validation.constant.StandardID.L12;
import static org.plateaubuilder.validation.constant.StandardID.L13;
import static org.plateaubuilder.validation.constant.StandardID.L14;
import static org.plateaubuilder.validation.constant.StandardID.L18;
import static org.plateaubuilder.validation.constant.StandardID.L_BLDG_02;
import static org.plateaubuilder.validation.constant.StandardID.L_BLDG_03;
import static org.plateaubuilder.validation.constant.StandardID.T03;
import static org.plateaubuilder.validation.constant.StandardID.T_BLDG_02;

public class ValidationCli {
    private static final Pattern EPSG_PATTERN = Pattern.compile("^EPSG:\\d+$", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String SCHEMA_LANDMARK_FILENAME = "urbanObject.xsd";
    private static final String SCHEMA_LANDMARK_PARENT_DIR = "schemas/iur/uro";
    private static final String PARAM_RESOURCE_PATH = "/org/plateaubuilder/validation/validation-params.json";

    public static void main(String[] args) {
        try {
            CliArgs cliArgs = parseArgs(args);

            if (!cliArgs.verbose) {
                Logger.getLogger("org.plateaubuilder.validation").setLevel(Level.OFF);
            }

            initializeJavaFxHeadless();

            Path inputPath = Paths.get(cliArgs.inputPath).toAbsolutePath().normalize();
            if (!Files.exists(inputPath) || !Files.isRegularFile(inputPath)) {
                fail("Input GML not found: " + inputPath);
                return;
            }

            Path logDir = null;
            if (cliArgs.saveLog) {
                Path datasetRoot = findDatasetRoot(inputPath);
                if (datasetRoot == null) {
                    fail("Schema file not found while searching upward from input: " + SCHEMA_LANDMARK_PARENT_DIR + "/*/" + SCHEMA_LANDMARK_FILENAME);
                    return;
                }
                logDir = datasetRoot.resolve(AppConst.VALIDATION_LOG_DESTINATION_DIRECTORY);
            }

            World.setActiveInstance(new World(), new Group()); // Non-obvious: ThreeDUtil.distance() relies on World.getActiveInstance()
            var cityModelView = GmlImporter.loadGmlHeadless(inputPath.toString(), cliArgs.epsg);
            if (cityModelView == null) {
                fail("Failed to load CityModel from input: " + inputPath);
                return;
            }

            List<IValidator> validators = loadValidators();
            if (validators.isEmpty()) {
                System.out.println("No validators selected.");
            } else {
                System.out.println("Validators to run (" + validators.size() + "):");
                for (IValidator validator : validators) {
                    System.out.println(" - " + validator.getClass().getSimpleName());
                }
            }

            List<ValidationResultMessage> allMessages = new ArrayList<>();
            int errorCount = 0;
            int warningCount = 0;

            for (IValidator validator : validators) {
                String validatorName = validator.getClass().getSimpleName();
                try {
                    List<ValidationResultMessage> messages = validator.validate(cityModelView);
                    for (ValidationResultMessage message : messages) {
                        if (message.getType() == ValidationResultMessageType.Error) {
                            errorCount++;
                        } else if (message.getType() == ValidationResultMessageType.Warning) {
                            warningCount++;
                        }
                    }
                    allMessages.addAll(messages);
                } catch (Exception ex) {
                    String crashMessage = validatorName + " crashed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    ValidationResultMessage synthetic = new ValidationResultMessage(
                            ValidationResultMessageType.Error,
                            crashMessage
                    );
                    errorCount++;
                    allMessages.add(synthetic);
                    System.err.println(crashMessage);
                }
            }

            System.out.println("品質検査が完了しました。(エラー数:" + errorCount + ",警告数:" + warningCount + ")");
            if (cliArgs.saveLog) {
                Path logFile = writeValidationLog(logDir, inputPath, cliArgs.epsg, errorCount, warningCount, allMessages);
                System.out.println("Log file: " + logFile.toAbsolutePath());
            }
            System.exit(0);
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
        } catch (Exception e) {
            fail("Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void initializeJavaFxHeadless() {
        System.setProperty("prism.order", "sw");
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
        }
    }

    private static CliArgs parseArgs(String[] args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException(
                    "Usage: :plateaubuilder-validation:run --args=\"<gml-path> [EPSG:xxxx] [--verbose|-v] [--save-log]\""
            );
        }

        String inputPath = args[0];
        String epsg = "EPSG:6677";
        boolean verbose = false;
        boolean saveLog = false;

        for (String token : Arrays.asList(args).subList(1, args.length)) {
            if (EPSG_PATTERN.matcher(token).matches()) {
                epsg = token.toUpperCase(Locale.ROOT);
            } else if ("--verbose".equalsIgnoreCase(token) || "-v".equalsIgnoreCase(token)) {
                verbose = true;
            } else if ("--save-log".equalsIgnoreCase(token)) {
                saveLog = true;
            }
        }

        return new CliArgs(inputPath, epsg, verbose, saveLog);
    }

    private static Path findDatasetRoot(Path inputPath) {
        Path current = inputPath.toAbsolutePath().normalize().getParent();
        while (current != null) {
            Path uroDir = current.resolve(SCHEMA_LANDMARK_PARENT_DIR);
            if (Files.isDirectory(uroDir)) {
                try (var versionDirs = Files.list(uroDir)) {
                    boolean found = versionDirs
                            .filter(Files::isDirectory)
                            .anyMatch(v -> Files.isRegularFile(v.resolve(SCHEMA_LANDMARK_FILENAME)));
                    if (found) {
                        return current;
                    }
                } catch (IOException ignored) {
                }
            }
            current = current.getParent();
        }
        return null;
    }

    private static List<IValidator> loadValidators() throws IOException {
        List<Standard> standards = readValidationParams();
        List<IValidator> validators = new ArrayList<>();
        for (Standard standard : standards) {
            String id = standard.getId();
            if (!standard.isEnabled()) {
                continue;
            }
            Supplier<IValidator> supplier = createValidatorSupplier(id);
            if (supplier == null) {
                System.out.println("WARN: Unsupported validator id in " + AppConst.VALIDATION_PARAM_FILE_NAME + ": " + id + " (skipped)");
                continue;
            }

            validators.add(supplier.get());
        }

        return validators;
    }

    private static List<Standard> readValidationParams() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream input = ValidationCli.class.getResourceAsStream(PARAM_RESOURCE_PATH)) {
            if (input == null) {
                throw new IOException("Validation params resource not found: " + PARAM_RESOURCE_PATH);
            }
            return mapper.readValue(input, new TypeReference<List<Standard>>() {
            });
        }
    }

    private static Supplier<IValidator> createValidatorSupplier(String id) {
        switch (id) {
            case C01:
                return GMLIDCompletenessValidator::new;
            case C04:
                return C04CompletenessValidator::new;
            case L04:
                return L04LogicalConsistencyValidator::new;
            case L05:
                return L05LogicalConsistencyValidator::new;
            case L06:
                return L06LogicalConsistencyValidator::new;
            case L07:
                return L07LogicalConsistencyValidator::new;
            case L08:
                return L08LogicalConsistencyValidator::new;
            case L09:
                return L09LogicalConsistencyValidator::new;
            case L10:
                return L10LogicalConsistencyValidator::new;
            case L11:
                return L11LogicalConsistencyValidator::new;
            case L12:
                return L12LogicalConsistencyValidator::new;
            case L13:
                return L13LogicalConsistencyValidator::new;
            case L14:
                return L14LogicalAccuaracyValidator::new;
            case L18:
                return L18LogicalConsistencyValidator::new;
            case L_BLDG_02:
                return Lbldg02LogicalConsistencyValidator::new;
            case L_BLDG_03:
                return Lbldg03LogicalAccuaracyValidator::new;
            case T03:
                return T03ThematicAccuaracyValidator::new;
            case T_BLDG_02:
                return Tbldg02ThematicAccuaracyValidator::new;
            case C_BLDG_01:
                return Lbldg01LogicalAccuracyValidator::new;
            default:
                return null;
        }
    }

    private static Path writeValidationLog(
            Path logDir,
            Path inputPath,
            String epsg,
            int errorCount,
            int warningCount,
            List<ValidationResultMessage> messages
    ) throws IOException {
        Files.createDirectories(logDir);
        String fileName = "validation-" + LOG_TIME_FORMATTER.format(LocalDateTime.now()) + ".log";
        Path logPath = logDir.resolve(fileName);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(logPath), StandardCharsets.UTF_8))) {
            writer.write("input=" + inputPath);
            writer.newLine();
            writer.write("epsg=" + epsg);
            writer.newLine();
            writer.write("errors=" + errorCount + ",warnings=" + warningCount);
            writer.newLine();
            writer.newLine();

            for (ValidationResultMessage message : messages) {
                writer.write("[" + message.getType() + "] " + message.getMessage());
                writer.newLine();
            }
        }

        return logPath;
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }

    private static class CliArgs {
        private final String inputPath;
        private final String epsg;
        private final boolean verbose;
        private final boolean saveLog;

        private CliArgs(String inputPath, String epsg, boolean verbose, boolean saveLog) {
            this.inputPath = inputPath;
            this.epsg = epsg;
            this.verbose = verbose;
            this.saveLog = saveLog;
        }
    }
}
