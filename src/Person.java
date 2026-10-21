public final class Person {
    public static final int MAX_NAME_LENGTH = 60;
    public static final int MAX_AGE = 130;

    private String nome;
    private int idade;

    public Person(String nome, int idade) {
        setNome(nome);
        setIdade(idade);
    }

    public String getNome() {
        return nome;
    }

    // "  Maria   Silva " -> "Maria Silva"
    public void setNome(String nome) {
        String cleaned = nome == null ? "" : nome.strip().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Informe o nome da pessoa.");
        }
        if (cleaned.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("O nome pode ter no máximo " + MAX_NAME_LENGTH + " caracteres.");
        }
        this.nome = cleaned;
    }

    public int getIdade() {
        return idade;
    }

    public void setIdade(int idade) {
        if (idade < 0 || idade > MAX_AGE) {
            throw new IllegalArgumentException("A idade deve estar entre 0 e " + MAX_AGE + " anos.");
        }
        this.idade = idade;
    }

    @Override
    public String toString() {
        return nome + " (" + idade + (idade == 1 ? " ano)" : " anos)");
    }
}
