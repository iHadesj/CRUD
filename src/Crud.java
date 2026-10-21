import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Crud {
    private final List<Person> data;
    private final PersonStore store;

    // só memória, nada vai pro arquivo
    public Crud() {
        this.data = new ArrayList<>();
        this.store = null;
    }

    public Crud(PersonStore store) throws IOException {
        this.data = store.load();
        this.store = store;
    }

    public void create(Person person) {
        data.add(person);
        save();
    }

    public List<Person> read() {
        return Collections.unmodifiableList(data);
    }

    public int size() {
        return data.size();
    }

    public boolean update(int index, Person newPerson) {
        if (!isValidIndex(index)) {
            return false;
        }
        data.set(index, newPerson);
        save();
        return true;
    }

    public boolean delete(int index) {
        if (!isValidIndex(index)) {
            return false;
        }
        data.remove(index);
        save();
        return true;
    }

    public boolean isPersistent() {
        return store != null;
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < data.size();
    }

    // se der erro ao gravar, a mudança fica só em memória e quem chamou trata a exceção
    private void save() {
        if (store == null) {
            return;
        }
        try {
            store.save(data);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar em " + store.getFile(), e);
        }
    }
}
