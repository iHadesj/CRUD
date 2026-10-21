import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

// CSV com ";" (padrão do Excel pt-BR), ex.: "Maria Silva";25
public class PersonStore {
    private static final String HEADER = "nome;idade";
    // sem BOM o Excel abre como ANSI e quebra os acentos
    private static final char BOM = '\uFEFF';

    private final Path file;

    public PersonStore(Path file) {
        this.file = file;
    }

    // ~/.crud-pessoas/pessoas.csv
    public static PersonStore inUserHome() {
        return new PersonStore(Path.of(System.getProperty("user.home"), ".crud-pessoas", "pessoas.csv"));
    }

    public Path getFile() {
        return file;
    }

    public List<Person> load() throws IOException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        return parse(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    public void save(List<Person> people) throws IOException {
        write(file, people);
    }

    // grava num .tmp e troca no final, pra não corromper o CSV se o programa cair no meio
    public static void write(Path target, List<Person> people) throws IOException {
        Path dir = target.toAbsolutePath().getParent();
        Files.createDirectories(dir);

        StringBuilder csv = new StringBuilder().append(BOM).append(HEADER).append('\n');
        for (Person person : people) {
            csv.append('"').append(person.getNome().replace("\"", "\"\"")).append('"')
                    .append(';').append(person.getIdade()).append('\n');
        }

        Path temp = Files.createTempFile(dir, "pessoas", ".tmp");
        try {
            Files.writeString(temp, csv, StandardCharsets.UTF_8);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static List<Person> parse(List<String> lines) throws IOException {
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = i == 0 && lines.get(0).startsWith(String.valueOf(BOM))
                    ? lines.get(0).substring(1)
                    : lines.get(i);
            if (line.isBlank() || (i == 0 && line.equalsIgnoreCase(HEADER))) {
                continue;
            }
            try {
                people.add(parseLine(line));
            } catch (IllegalArgumentException e) {
                throw new IOException("Linha " + (i + 1) + " inválida: " + e.getMessage(), e);
            }
        }
        return people;
    }

    private static Person parseLine(String line) {
        int separator = line.lastIndexOf(';');
        if (separator < 0) {
            throw new IllegalArgumentException("faltou o \";\" entre nome e idade.");
        }

        String name = line.substring(0, separator).strip();
        if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
            name = name.substring(1, name.length() - 1).replace("\"\"", "\"");
        }

        int age;
        try {
            age = Integer.parseInt(line.substring(separator + 1).strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("a idade não é um número.");
        }
        return new Person(name, age);
    }
}
