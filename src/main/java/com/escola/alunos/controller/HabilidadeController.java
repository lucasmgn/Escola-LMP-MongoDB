package com.escola.alunos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.escola.alunos.model.Aluno;
import com.escola.alunos.model.Habilidade;
import com.escola.alunos.repository.AlunoRepository;

@Controller
public class HabilidadeController {

	@Autowired
	private AlunoRepository repository;

	@GetMapping("/habilidades/cadastrarHabilidade/{id}")
	public String cadastrar(@PathVariable String id, Model model) {
		Aluno aluno = repository.obterAlunoId(id);
		model.addAttribute("aluno", aluno);
		model.addAttribute("habilidade", new Habilidade());
		return "habilidades/cadastrarHabilidade";
	}
	
	//Salvando nova habilidade de um aluno
	@PostMapping("/habilidades/salvar/{id}")
	public String salvar(@PathVariable String id, @ModelAttribute Habilidade habilidade) {
		Aluno aluno = repository.obterAlunoId(id);
		repository.salvar(aluno.adicionarHabilidade(aluno, habilidade)); 
		return "redirect:/aluno/listar";
	}
}
