package br.com.aluracursos.literalura.service;

import br.com.aluracursos.literalura.dto.DadosAutorDTO;
import br.com.aluracursos.literalura.dto.DadosLivroDTO;
import br.com.aluracursos.literalura.dto.ResultadoBuscaDTO;
import br.com.aluracursos.literalura.model.Autor;
import br.com.aluracursos.literalura.model.Livro;
import br.com.aluracursos.literalura.repository.AutorRepository;
import br.com.aluracursos.literalura.repository.LivroRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Service
public class LivroService {

    private final LivroRepository livroRepository;
    private final AutorRepository autorRepository;

    public LivroService(LivroRepository livroRepository, AutorRepository autorRepository) {
        this.livroRepository = livroRepository;
        this.autorRepository = autorRepository;
    }

    public void buscarLivropeloTitulo(Scanner scanner) {
        System.out.print("Digite o título do livro: ");
        String titulo = scanner.nextLine();
        String url = "https://gutendex.com/books/?search=" + titulo.replace(" ", "%20");
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResultadoBuscaDTO resultado = restTemplate.getForObject(url, ResultadoBuscaDTO.class);
            if (resultado != null && resultado.temResultados()) {
                List<DadosLivroDTO> livrosEncontrados = resultado.getResultados();
                System.out.println("\nResultados encontrados:");
                for (int i = 0; i < livrosEncontrados.size(); i++) {
                    DadosLivroDTO livroApi = livrosEncontrados.get(i);
                    String nomeAutor = (livroApi.autores() != null && !livroApi.autores().isEmpty()) ? livroApi.autores().get(0).getNome() : "Desconhecido";
                    String idiomas = (livroApi.idiomas() != null && !livroApi.idiomas().isEmpty()) ? String.join(", ", livroApi.idiomas()) : "desconhecido";
                    System.out.printf("[%d] Título: %s | Autor: %s | Idioma(s): %s | Downloads: %d\n", i + 1, livroApi.titulo(), nomeAutor, idiomas, livroApi.numeroDownloads());
                }
                System.out.print("Escolha o número do livro para cadastrar, ou 0 para cancelar: ");
                int escolha = scanner.nextInt();
                scanner.nextLine();
                if (escolha < 1 || escolha > livrosEncontrados.size()) {
                    System.out.println("Operação cancelada ou escolha inválida.");
                    return;
                }
                DadosLivroDTO livroEscolhido = livrosEncontrados.get(escolha - 1);
                DadosAutorDTO autorApi;
                if (livroEscolhido.autores() != null && !livroEscolhido.autores().isEmpty()) {
                    autorApi = livroEscolhido.autores().get(0);
                } else {
                    autorApi = new DadosAutorDTO("Autor desconhecido", null, null);
                }
                Optional<Autor> autorOpt = autorRepository.findByNome(autorApi.getNome());
                Autor autor = autorOpt.orElseGet(() -> {Autor novoAutor = new Autor(autorApi.getNome(),
                                                                                    autorApi.getAnoNascimento(),
                                                                                    autorApi.getAnoFalecimento()
                    );
                    return autorRepository.save(novoAutor);
                });
                boolean existe = livroRepository.existsByTituloAndAutor_Nome(livroEscolhido.titulo(), autor.getNome());
                if (existe) {
                    System.out.println("Este livro já está cadastrado no banco de dados.");
                } else {
                    String idioma = (livroEscolhido.idiomas() != null && !livroEscolhido.idiomas().isEmpty()) ? livroEscolhido.idiomas().get(0) : "desconhecido";
                    Livro livro = new Livro(livroEscolhido.titulo(),
                                            idioma,
                                            livroEscolhido.numeroDownloads(),
                                            autor
                    );
                    livroRepository.save(livro);
                    System.out.println("Livro cadastrado com sucesso!");
                }
            } else {
                System.out.println("Livro não encontrado.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao consultar API: " + e.getMessage());
        }
    }

    public void listarAutores() {
        List<Autor> autores = autorRepository.findAll();
        if (autores.isEmpty()) {
            System.out.println("Nenhum autor cadastrado.");
        } else {
            autores.forEach(System.out::println);
        }
    }

    public void listarLivrosRegistrados() {
        List<Livro> livros = livroRepository.findAll();
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro registrado.");
        } else {
            livros.forEach(System.out::println);
        }
    }

    public void listarAutoresemDeterminadoAno(Scanner scanner) {
        System.out.print("Digite o ano: ");
        int anoEscolhido = scanner.nextInt();
        scanner.nextLine();
        List<Autor> autores = autorRepository.findAutoresVivosNoAno(anoEscolhido);
        if (autores.isEmpty()) {
            System.out.println("Nenhum autor vivo encontrado nesse ano.");
        } else {
            autores.forEach(System.out::println);
        }
    }

    public void listarLivrosemDeterminadoIdioma(Scanner scanner) {
        System.out.print("Digite o idioma (ex: pt, en, es, fr): ");
        String idioma = scanner.nextLine().trim().toLowerCase();
        List<Livro> livros = livroRepository.findByIdiomaIgnoreCase(idioma);
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro encontrado nesse idioma.");
        } else {
            livros.forEach(System.out::println);
        }
    }
}