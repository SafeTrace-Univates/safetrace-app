# SafeTrace - Documentação Completa

## 📱 Visão Geral

SafeTrace é um aplicativo Android desenvolvido para segurança pessoal e emergências. O app permite que usuários cadastrem contatos de confiança, acionem emergências com gravação de áudio e rastreamento de localização em tempo real, e visualizem o histórico completo de emergências acionadas.

## 🎯 Funcionalidades Principais

### 1. Sistema de Autenticação

#### Login
- **Tela de Login** (`LoginActivity`)
  - Campos: Email e Senha
  - Validação de campos obrigatórios
  - Toggle de visibilidade de senha (mostrar/ocultar)
  - Integração com API para autenticação
  - Salvamento automático de token de sessão
  - **Auto-login**: Se o usuário já tiver uma sessão ativa (token salvo), o app redireciona automaticamente para a tela principal sem exigir novo login

#### Cadastro de Usuário
- **Tela de Cadastro** (`CadastroActivity`)
  - Campos: Nome Completo, Email, CPF, Telefone e Senha
  - Validação de todos os campos
  - Toggle de visibilidade de senha
  - Botão de voltar para tela de login
  - Integração com API para registro
  - Salvamento automático de dados do usuário após cadastro

### 2. Tela Principal (Home)

- **MainActivity**
  - Botão de Emergência (botão principal, central)
    - Estado normal: Texto "Emergência", cor primária
    - Estado ativo: Texto "Finalizar Emergência", cor `#CE7E79` (emergencia_ativa)
    - Ao clicar:
      - **Se não houver emergência ativa**: Inicia gravação de áudio, rastreamento de localização em tempo real e notificações aos contatos cadastrados
      - **Se houver emergência ativa**: Mostra diálogo de confirmação para finalizar a emergência
  - Botões de Serviços de Emergência:
    - Polícia Militar (190)
    - Defesa da Mulher
    - SAMU (192)
    - Bombeiros (193)
    - Polícia Civil
    - Defesa Civil (199)
    - Ao clicar, abre o discador do telefone com o número correspondente
  - Menu de navegação lateral (Drawer)
    - Opções: Home, Contatos de Confiança, Histórico

### 3. Gerenciamento de Contatos de Confiança

- **CadastroContatosActivity**
  - **Adicionar Contato Manualmente**:
    - Campo para digitar código do usuário
    - Botão "Salvar"
    - Integração com API para adicionar contato usando código
  - **Gerar Meu QR Code/Código**:
    - Gera QR code com o ID do usuário atual
    - Exibe popup com:
      - Imagem do QR code
      - Código em formato texto abaixo do QR code
      - Botão "Copiar" para copiar o código
      - Botão de fechar
  - **Adicionar Contatos por QR Code**:
    - Abre câmera para escanear QR code
    - Leitura automática usando ML Kit Barcode Scanning
    - Após escanear, preenche automaticamente o campo de código e adiciona o contato
  - **Lista de Contatos**:
    - Carrega contatos da API
    - Exibe nome ou nickname de cada contato
    - Botões para ligar diretamente para cada contato

### 4. Sistema de Emergência

#### Funcionalidades
- **Gravação de Áudio**:
  - Inicia gravação quando emergência é acionada
  - Grava em formato padrão do Android
  - Arquivo salvo no armazenamento interno do app
  - Continua gravando mesmo com app em background (Foreground Service)

- **Rastreamento de Localização**:
  - Rastreamento em tempo real usando GPS
  - Atualizações a cada 3-5 segundos
  - Alta precisão (PRIORITY_HIGH_ACCURACY)
  - Continua rastreando mesmo com app em background (Foreground Service)
  - Salva todas as localizações com timestamp e precisão

- **Dados Capturados**:
  - Data e hora de início da emergência
  - Data e hora de fim da emergência
  - Nome do usuário que acionou
  - Lista de contatos que receberam notificação
  - Todas as localizações registradas durante a emergência
  - Arquivo de áudio completo da gravação

- **Notificação Silenciosa**:
  - Notificação persistente enquanto emergência está ativa
  - Notificação silenciosa (sem som, vibração ou luz)
  - Exibe nome do usuário e status "Gravando áudio e localização"
  - Permanece visível mesmo quando app está fechado

### 5. Histórico de Emergencias

- **HistoricoActivity**
  - Lista todas as emergências registradas
  - **Ordenação**: Mais recentes primeiro, mais antigas por último
  - Para cada emergência, exibe:
    - Data (dd/MM/yyyy)
    - Hora de início (HH:mm)
    - Hora de fim (HH:mm) ou "Em andamento"
    - Nome do usuário que acionou
    - Lista de contatos que receberam notificação
  - **Botão "Ver mais"** em cada card:
    - Abre `TrajetoActivity` com mapa e detalhes completos

### 6. Visualização de Trajeto (Mapa)

- **TrajetoActivity**
  - **Mapa Google Maps**:
    - Exibe trajeto completo da emergência
    - Linha vermelha conectando todas as localizações
    - Marcador "Início" na primeira localização
    - Marcador "Fim" na última localização
    - Zoom automático para mostrar todo o trajeto
  - **Dados da Emergência**:
    - Data/Hora de início formatada
    - Data/Hora de fim formatada
    - Duração calculada (minutos e segundos)
    - Nome do usuário
    - Lista de contatos notificados
    - Quantidade de localizações registradas
    - Tamanho do arquivo de áudio (se disponível)
  - **Reprodução de Áudio**:
    - Botão "Reproduzir Áudio" / "Pausar Áudio"
    - Reproduz o áudio gravado durante a emergência
    - Controle de play/pause
    - Auto-pausa quando áudio termina
    - Verifica se arquivo existe antes de reproduzir
    - Desabilita botão se áudio não estiver disponível

## 🔧 Tecnologias e Bibliotecas Utilizadas

### Android Core
- **Material Design Components**: Interface moderna
- **AndroidX**: Biblioteca de compatibilidade
- **ConstraintLayout**: Layouts flexíveis
- **DrawerLayout**: Menu lateral de navegação

### Localização e Mapas
- **Google Maps SDK for Android**: Visualização de mapas
- **Google Play Services Location**: Rastreamento de localização GPS
- **FusedLocationProviderClient**: Obtenção de localização com alta precisão

### Multimídia
- **MediaRecorder**: Gravação de áudio
- **MediaPlayer**: Reprodução de áudio

### QR Code
- **ML Kit Barcode Scanning**: Escaneamento de QR codes via câmera
- **CameraX**: Gerenciamento de câmera
- **ZXing**: Geração de QR codes

### API e Comunicação
- **Volley**: Requisições HTTP para API REST
- **JSON**: Serialização e deserialização de dados

### Armazenamento
- **SharedPreferences**: Armazenamento local de dados (token, user_id, user_name, emergências)
- **File Storage**: Armazenamento de arquivos de áudio

### Serviços
- **ForegroundService**: Execução em background para gravação contínua
- **NotificationManager**: Gerenciamento de notificações

## 📂 Estrutura de Dados

### Modelo: Emergencia
```java
- id: String (UUID)
- dataInicio: Date
- dataFim: Date
- usuarioId: String
- usuarioNome: String
- notificadosIds: List<String>
- notificadosNomes: List<String>
- localizacoes: List<Localizacao>
- caminhoAudio: String
- emAndamento: boolean
```

### Modelo: Localizacao
```java
- latitude: double
- longitude: double
- timestamp: Date
- precisao: float (em metros)
```

## 🔐 Permissões Necessárias

1. **INTERNET**: Comunicação com API
2. **CAMERA**: Escaneamento de QR codes
3. **RECORD_AUDIO**: Gravação de áudio durante emergências
4. **ACCESS_FINE_LOCATION**: Rastreamento preciso de localização
5. **ACCESS_COARSE_LOCATION**: Rastreamento básico de localização
6. **ACCESS_BACKGROUND_LOCATION**: Continuar rastreamento em background
7. **POST_NOTIFICATIONS**: Exibir notificações
8. **FOREGROUND_SERVICE**: Executar serviço em foreground
9. **FOREGROUND_SERVICE_MICROPHONE**: Gravação em foreground
10. **FOREGROUND_SERVICE_LOCATION**: Localização em foreground

## 📱 Telas (Activities)

1. **LoginActivity**: Tela inicial de login
2. **CadastroActivity**: Cadastro de novos usuários
3. **MainActivity**: Tela principal com botões de emergência
4. **CadastroContatosActivity**: Gerenciamento de contatos de confiança
5. **HistoricoActivity**: Histórico de emergências acionadas
6. **ScanQRActivity**: Scanner de QR codes
7. **TrajetoActivity**: Visualização de trajeto no mapa

## 🛠️ Serviços (Services)

1. **EmergenciaService**: Singleton para gerenciar emergências
   - Iniciar/finalizar emergências
   - Gravar áudio
   - Salvar/carregar emergências do SharedPreferences
   - Adicionar localizações

2. **LocationService**: Singleton para rastreamento GPS
   - Iniciar/parar rastreamento
   - Receber atualizações de localização
   - Adicionar localizações ao EmergenciaService

3. **EmergenciaForegroundService**: Serviço em foreground
   - Executa gravação e rastreamento em background
   - Mantém notificação persistente
   - Continua funcionando mesmo com app fechado

## 🌐 Integração com API

### Endpoints Utilizados
- **POST** `/api/v1/auth/login`: Autenticação de usuário
- **POST** `/api/v1/auth/register`: Registro de novo usuário
- **GET** `/api/v1/user/profile`: Obter perfil do usuário
- **GET** `/api/v1/contacts`: Listar contatos de confiança
- **POST** `/api/v1/contacts`: Adicionar contato por código

### Autenticação
- Token JWT armazenado em SharedPreferences
- Token enviado em header `Authorization: Bearer {token}`
- Validação automática de sessão no login

## 💾 Persistência de Dados

### SharedPreferences (safetrace_prefs)
- `api_token`: Token de autenticação
- `user_id`: ID do usuário logado
- `user_name`: Nome do usuário logado
- `emergencias`: JSON array com todas as emergências salvas
- `temp_contatos_ids`: IDs temporários de contatos para emergência
- `temp_contatos_nomes`: Nomes temporários de contatos para emergência

### Arquivos
- Áudios gravados salvos em diretório interno do app
- Caminho do arquivo salvo junto com dados da emergência

## 🎨 Interface do Usuário

### Cores Principais
- **Primária**: `#ce817b` (Rosa)
- **Secundária**: `#BA0606` (Vermelho)
- **Emergência Ativa**: `#CE7E79` (Rosa claro)
- **Fundo**: `#ffe9e9` (Rosa muito claro)

### Fontes
- **Poppins**: Fonte principal
- **Poppins SemiBold**: Títulos e textos importantes

### Componentes
- Material Design Components
- Cards com bordas arredondadas
- Botões com estilo Material
- Navigation Drawer lateral
- Diálogos Material Design

## ⚠️ Tratamento de Erros

- Validação de campos obrigatórios
- Mensagens de erro amigáveis do usuário
- Tradução de mensagens de erro da API
- Try-catch em operações críticas
- Logs detalhados para depuração
- Fallbacks quando serviços não estão disponíveis

## 📊 Fluxo de Uso

### Primeiro Uso
1. Usuário abre o app
2. Tela de login aparece
3. Clica em "Cadastrar" (se novo usuário)
4. Preenche dados e cadastra
5. Volta para login e faz login

### Uso Diário
1. App abre diretamente na tela principal (se já logado)
2. Usuário cadastra contatos de confiança (QR code ou código manual)
3. Em situação de emergência:
   - Clica no botão "Emergência"
   - App inicia gravação e rastreamento
   - Contatos recebem notificação
   - Usuário pode usar botões de serviços de emergência (190, 192, etc.)
   - Ao finalizar, clica novamente no botão
4. Visualiza histórico de emergências
5. Acessa detalhes completos com mapa e áudio

## 🔄 Recursos em Background

- Gravação continua mesmo com app fechado
- Rastreamento GPS continua em background
- Notificação persistente mantém usuário informado
- Dados salvos automaticamente quando emergência é finalizada

## 📝 Notas Técnicas

- Uso de reflection para Google Maps (evita erros de compilação se SDK não estiver disponível)
- Singleton pattern para serviços (EmergenciaService, LocationService)
- ActivityResultLauncher para comunicação entre Activities
- Foreground Service para garantir execução contínua
- Validação de permissões em runtime (Android 6.0+)
- Suporte a Android 5.0+ (minSdk 28)

## 🚀 Próximas Melhorias Sugeridas

- Notificações push para contatos
- Upload automático de emergências para servidor
- Sincronização de dados entre dispositivos
- Modo escuro
- Suporte a múltiplos idiomas
- Estatísticas e relatórios
- Integração com wearables

