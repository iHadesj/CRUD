import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

// GET    /pessoas      lista
// GET    /pessoas/1    busca pelo id
// POST   /pessoas      cria      body: nome=Maria&idade=25
// PUT    /pessoas/1    atualiza  body: nome=Maria&idade=26
// DELETE /pessoas/1    remove
public class CrudServer implements HttpHandler {
    private Crud crud = CrudExecutor.openCrud();

    public static void main(String[] args) throws IOException {
        // 8080 geralmente tá com o Apache do XAMPP
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8081), 0);
        server.createContext("/pessoas", new CrudServer());
        server.start();
        System.out.println("Servidor rodando em http://localhost:8081/pessoas");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/pessoas")) {
                if (method.equals("GET")) {
                    listAll(exchange);
                } else if (method.equals("POST")) {
                    crud.create(readPerson(exchange));
                    send(exchange, 201, "Pessoa criada com id " + crud.size() + ".");
                } else {
                    send(exchange, 405, "Método não permitido.");
                }
                return;
            }

            int index = readIndex(path);
            if (index == -1) {
                send(exchange, 404, "Pessoa não encontrada.");
            } else if (method.equals("GET")) {
                send(exchange, 200, crud.read().get(index).toString());
            } else if (method.equals("PUT")) {
                crud.update(index, readPerson(exchange));
                send(exchange, 200, "Pessoa atualizada.");
            } else if (method.equals("DELETE")) {
                crud.delete(index);
                send(exchange, 200, "Pessoa removida.");
            } else {
                send(exchange, 405, "Método não permitido.");
            }
        } catch (IllegalArgumentException e) {
            // validação da Person
            send(exchange, 400, e.getMessage());
        } catch (Exception e) {
            send(exchange, 500, "Erro no servidor: " + e.getMessage());
        }
    }

    private void listAll(HttpExchange exchange) throws IOException {
        List<Person> persons = crud.read();
        if (persons.isEmpty()) {
            send(exchange, 200, "Nenhuma pessoa cadastrada.");
            return;
        }
        String text = "";
        for (int i = 0; i < persons.size(); i++) {
            text = text + (i + 1) + ". " + persons.get(i) + "\n";
        }
        send(exchange, 200, text);
    }

    // "/pessoas/3" -> índice 2, ou -1 se não existir
    private int readIndex(String path) {
        if (!path.startsWith("/pessoas/")) {
            return -1;
        }
        String id = path.substring("/pessoas/".length());
        try {
            int index = Integer.parseInt(id) - 1;
            if (index >= 0 && index < crud.size()) {
                return index;
            }
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // body: nome=Maria&idade=25
    private Person readPerson(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");

        String nome = null;
        String idade = null;
        for (String part : body.split("&")) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2) {
                if (keyValue[0].equals("nome")) {
                    nome = keyValue[1];
                } else if (keyValue[0].equals("idade")) {
                    idade = keyValue[1];
                }
            }
        }

        try {
            return new Person(nome, Integer.parseInt(idade));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Informe a idade como número inteiro.");
        }
    }

    private void send(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream out = exchange.getResponseBody();
        out.write(bytes);
        out.close();
    }
}
