import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class CrudExecutor {
    private final Crud crud;
    private final Scanner scanner;

    public CrudExecutor() {
        this.crud = openCrud();
        this.scanner = new Scanner(System.in);
    }


    // o CrudServer usa também, assim os dois mexem no mesmo CSV
    static Crud openCrud() {
        PersonStore store = PersonStore.inUserHome();
        try {
            return new Crud(store);
        } catch (IOException e) {
            System.out.println("Não foi possível ler " + store.getFile() + ": " + e.getMessage());
            System.out.println("Rodando em modo temporário (nada será salvo).");
            return new Crud();
        }
    }

    public void execute() {
        try {
            while (true) {
                System.out.println();
                System.out.println("Escolha uma opção:");
                System.out.println("1. Adicionar pessoa");
                System.out.println("2. Ver todas as pessoas");
                System.out.println("3. Atualizar pessoa");
                System.out.println("4. Remover pessoa");
                System.out.println("5. Sair");

                switch (readInt("> ")) {
                    case 1 -> addPerson();
                    case 2 -> viewAllPersons();
                    case 3 -> updatePerson();
                    case 4 -> removePerson();
                    case 5 -> {
                        return;
                    }
                    default -> System.out.println("Opção inválida.");
                }
            }
        } catch (NoSuchElementException e) {
            // stdin fechado (Ctrl+Z / Ctrl+D), só encerra
        }
    }

    private void addPerson() {
        Person person = readPerson("Digite o nome da pessoa:", "Digite a idade da pessoa:");
        save(() -> crud.create(person), "Pessoa adicionada.");
    }

    private void viewAllPersons() {
        List<Person> persons = crud.read();
        if (persons.isEmpty()) {
            System.out.println("Nenhuma pessoa cadastrada ainda.");
            return;
        }
        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);
            System.out.println((i + 1) + ". Nome: " + person.getNome() + ", Idade: " + person.getIdade());
        }
    }

    private void updatePerson() {
        int index = readIndex("Digite o número da pessoa que você deseja atualizar:");
        if (index < 0) {
            return;
        }

        System.out.println("Atualizando: " + crud.read().get(index));
        Person newPerson = readPerson("Digite o novo nome da pessoa:", "Digite a nova idade da pessoa:");
        save(() -> crud.update(index, newPerson), "Pessoa atualizada.");
    }

    private void removePerson() {
        int index = readIndex("Digite o número da pessoa que você deseja remover:");
        if (index < 0) {
            return;
        }

        Person person = crud.read().get(index);
        System.out.println("Remover " + person + "? (s/n)");
        if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
            save(() -> crud.delete(index), "Pessoa removida.");
        }
    }

    // -1 se a lista estiver vazia
    private int readIndex(String prompt) {
        if (crud.size() == 0) {
            System.out.println("Nenhuma pessoa cadastrada ainda.");
            return -1;
        }
        viewAllPersons();
        while (true) {
            int number = readInt(prompt + " ");
            if (number >= 1 && number <= crud.size()) {
                return number - 1;
            }
            System.out.println("Escolha um número entre 1 e " + crud.size() + ".");
        }
    }

    private Person readPerson(String namePrompt, String agePrompt) {
        while (true) {
            System.out.println(namePrompt);
            String nome = scanner.nextLine();
            int idade = readInt(agePrompt + " ");
            try {
                return new Person(nome, idade);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage() + " Tente de novo.");
            }
        }
    }

    // nextLine em vez de nextInt: com input inválido o nextInt deixa o token no buffer e entra em loop
    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Digite apenas números.");
            }
        }
    }

    private void save(Runnable change, String successMessage) {
        try {
            change.run();
            System.out.println(successMessage);
        } catch (UncheckedIOException e) {
            System.out.println("Alterado, mas não foi possível salvar no arquivo: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        CrudExecutor executor = new CrudExecutor();
        executor.execute();
    }
}
