package com.example.safetrace.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Emergencia {
    private String id;
    private Date dataInicio;
    private Date dataFim;
    private String usuarioId;
    private String usuarioNome;
    private List<String> notificadosIds; // IDs dos contatos que receberam notificação
    private List<String> notificadosNomes; // Nomes dos contatos que receberam notificação
    private List<Localizacao> localizacoes; // Lista de localizações durante a emergência
    private String caminhoAudio; // Caminho do arquivo de áudio
    private boolean emAndamento;
    
    public Emergencia() {
        this.localizacoes = new ArrayList<>();
        this.notificadosIds = new ArrayList<>();
        this.notificadosNomes = new ArrayList<>();
        this.emAndamento = true;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Date getDataInicio() {
        return dataInicio;
    }
    
    public void setDataInicio(Date dataInicio) {
        this.dataInicio = dataInicio;
    }
    
    public Date getDataFim() {
        return dataFim;
    }
    
    public void setDataFim(Date dataFim) {
        this.dataFim = dataFim;
    }
    
    public String getUsuarioId() {
        return usuarioId;
    }
    
    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }
    
    public String getUsuarioNome() {
        return usuarioNome;
    }
    
    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }
    
    public List<String> getNotificadosIds() {
        return notificadosIds;
    }
    
    public void setNotificadosIds(List<String> notificadosIds) {
        this.notificadosIds = notificadosIds;
    }
    
    public List<String> getNotificadosNomes() {
        return notificadosNomes;
    }
    
    public void setNotificadosNomes(List<String> notificadosNomes) {
        this.notificadosNomes = notificadosNomes;
    }
    
    public List<Localizacao> getLocalizacoes() {
        return localizacoes;
    }
    
    public void setLocalizacoes(List<Localizacao> localizacoes) {
        this.localizacoes = localizacoes;
    }
    
    public void adicionarLocalizacao(Localizacao localizacao) {
        this.localizacoes.add(localizacao);
    }
    
    public String getCaminhoAudio() {
        return caminhoAudio;
    }
    
    public void setCaminhoAudio(String caminhoAudio) {
        this.caminhoAudio = caminhoAudio;
    }
    
    public boolean isEmAndamento() {
        return emAndamento;
    }
    
    public void setEmAndamento(boolean emAndamento) {
        this.emAndamento = emAndamento;
    }
}

