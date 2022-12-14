package com.escola.alunos.repository;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import com.escola.alunos.codec.AlunoCodec;
import com.escola.alunos.model.Aluno;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

@Repository
public class AlunoRepository {

	private MongoClient cliente;
	private MongoDatabase bancoDeDados;

	public void salvar(Aluno aluno) {

		criarCodec();
		MongoCollection<Aluno> alunos = this.bancoDeDados.getCollection("alunos", Aluno.class);

		if (aluno.getId() == null) {
			alunos.insertOne(aluno);
		} else {
			alunos.updateOne(Filters.eq("_id", aluno.getId()), new Document("$set", aluno));
		}

		fecharConexao();
	}

	public List<Aluno> obterTodosAlunos() {

		criarCodec();
		MongoCollection<Aluno> alunos = this.bancoDeDados.getCollection("alunos", Aluno.class);
		MongoCursor<Aluno> iterator = alunos.find().iterator();

		List<Aluno> alunosEncontrados = popularAlunos(iterator);

		// Criando uma interação enquanto achar resultados
		while (iterator.hasNext()) {
			Aluno aluno = iterator.next();
			alunosEncontrados.add(aluno);
		}
		fecharConexao();

		return alunosEncontrados;
	}

	// Buscando aluno por id
	public Aluno obterAlunoId(String id) {
		criarCodec();
		// Utilizando a coleção alunos
		MongoCollection<Aluno> alunos = this.bancoDeDados.getCollection("alunos", Aluno.class);
		Aluno alunoId = alunos.find(Filters.eq("_id", new ObjectId(id))).first();

		fecharConexao();
		return alunoId;
	}
	
	public List<Aluno> obterAlunoNome(String nome) {
		criarCodec();
		// Utilizando a coleção alunos
		MongoCollection<Aluno> alunoCollection = this.bancoDeDados.getCollection("alunos", Aluno.class);
		MongoCursor<Aluno> resultados = alunoCollection.find(Filters.eq("nome", nome), Aluno.class).iterator();
		List<Aluno> alunos = popularAlunos(resultados);
		
		fecharConexao();
		return alunos;
	}
	
	public List<Aluno> obterAlunoNota(String classificacao, double nota) {
		
		criarCodec();
		
		MongoCollection<Aluno> alunoCollection = this.bancoDeDados.getCollection("alunos", Aluno.class);
		
		MongoCursor<Aluno> resultados = null;
		
		if(classificacao.equals("reprovados")) {
			resultados = alunoCollection.find(Filters.lt("notas",nota)).iterator();
		}else if(classificacao.equals("aprovados")) {
			resultados = alunoCollection.find(Filters.gte("notas",nota)).iterator();
		}
		
		List<Aluno> alunos = popularAlunos(resultados);
		
		fecharConexao();
		
		return alunos;
	}
	
	public List<Aluno> pesquisarPorGeolocalizacao(Aluno aluno) {
		criarCodec();
		
		MongoCollection<Aluno> alunoCollection = this.bancoDeDados.getCollection("alunos", Aluno.class);
		
		alunoCollection.createIndex(Indexes.geo2dsphere("contato"));
		
		List<Double> coordinates = aluno.getContato().getCoordinates();
		Point point = new Point(new Position(coordinates.get(0), coordinates.get(1)));
		
		//buscando os alunos que estão mais proximos, skip 1 para n pegar o mesmo aluno
		MongoCursor<Aluno> resultados = alunoCollection.find(
				Filters.nearSphere("contato", point, 2000.0, 0.0))
				.limit(2).skip(1).iterator();
		
		List<Aluno> alunos = popularAlunos(resultados);
		
		fecharConexao();
		
		return alunos;
	}

	private void fecharConexao() {
		this.cliente.close();
	}

	// Registrando um codec, conexão
	private void criarCodec() {
		Codec<Document> codec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);

		AlunoCodec alunoCodec = new AlunoCodec(codec);

		CodecRegistry registro = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
				CodecRegistries.fromCodecs(alunoCodec));

		MongoClientSettings build = MongoClientSettings.builder().codecRegistry(registro).build();
		this.cliente = MongoClients.create(build);
		// Se o DB não existirm ele cria um com o nome definido
		this.bancoDeDados = cliente.getDatabase("escola-LMP");
	}
	
	private List<Aluno> popularAlunos(MongoCursor<Aluno> resultados){
		List<Aluno> alunos = new ArrayList<>();
		while(resultados.hasNext()) {
			alunos.add(resultados.next());
		}
		return alunos;
	}
}
