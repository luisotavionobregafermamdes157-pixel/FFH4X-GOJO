package dev.gojo.client.gui;

import dev.gojo.client.config.ConfigManager;
import dev.gojo.client.config.ClientProfileManager;
import dev.gojo.client.config.ClientLanguage;
import dev.gojo.client.config.ClientOptimizer;
import dev.gojo.client.module.GojoModule;
import dev.gojo.client.module.ModuleCategory;
import dev.gojo.client.module.ModuleManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GojoScreen extends Screen {
    private static final int BG = 0xF0101018;
    private static final int PANEL = 0xFF191925;
    private static final int BLUE = 0xFF4B8DFF;
    private static final int PURPLE = 0xFF9A63FF;
    private static final int ROW_STEP = 58;
    private ModuleCategory selected = ModuleCategory.COMBAT;
    private TextFieldWidget search;
    private int panelX, panelY, panelW, panelH;
    private int scroll;
    private boolean optimized;
    private String cachedQuery = null;
    private List<GojoModule> cachedVisible = List.of();

    public GojoScreen() { super(Text.literal("GOJO CLIENT")); }

    @Override protected void init() {
        panelW = Math.max(320, Math.min(650, width - 16));
        panelH = Math.max(240, Math.min(390, height - 16));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        search = new TextFieldWidget(textRenderer, panelX + 178, panelY + 43, panelW - 292, 20, Text.literal(pt() ? "Buscar módulos" : "Search modules"));
        search.setPlaceholder(Text.literal(pt() ? "Buscar módulos..." : "Search modules..."));
        search.setChangedListener(value -> refreshFiltered());
        addDrawableChild(search);
        refreshFiltered();
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x99000000);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG);
        context.fill(panelX, panelY, panelX + panelW, panelY + 3, BLUE);
        context.fill(panelX, panelY + 3, panelX + panelW, panelY + 76, PANEL);
        context.drawText(textRenderer, "GOJO", panelX + 18, panelY + 14, 0xFFFFFFFF, true);
        context.drawText(textRenderer, "CLIENT  /  1.21.11", panelX + 68, panelY + 17, 0xFF9A9AAF, false);
        String playerStats = "JOGADOR: " + ClientProfileManager.getPlayerName()
                + "  |  ENTRADAS: " + ClientProfileManager.getLoginCount();
        context.drawText(textRenderer, playerStats, panelX + 18, panelY + 30, 0xFFB9B9D0, false);
        context.drawText(textRenderer, pt() ? "SHIFT DIREITO  ·  FECHAR" : "RIGHT SHIFT  ·  CLOSE", panelX + panelW - 145, panelY + 19, 0xFF77778A, false);
        context.fill(panelX + 14, panelY + 42, panelX + 162, panelY + 65, 0xFF222232);
        context.drawText(textRenderer, pt() ? "GERENCIADOR DE MÓDULOS" : "MODULE MANAGER", panelX + 22, panelY + 49, 0xFFB9B9D0, false);
        context.fill(panelX + panelW - 104, panelY + 42, panelX + panelW - 18, panelY + 65, 0xFF303047);
        String languageLabel = pt() ? "IDIOMA: PT" : "LANG: EN";
        int languageWidth = textRenderer.getWidth(languageLabel);
        context.drawText(textRenderer, languageLabel, panelX + panelW - 61 - languageWidth / 2, panelY + 49, 0xFF9EDBFF, true);
        context.fill(panelX + 14, panelY + 81, panelX + 146, panelY + panelH - 15, PANEL);
        int cy = panelY + 94;
        for (ModuleCategory category : ModuleCategory.values()) {
            boolean active = category == selected;
            if (active) context.fill(panelX + 19, cy - 5, panelX + 140, cy + 17, 0xFF2B2B49);
            context.fill(panelX + 19, cy - 5, panelX + 22, cy + 17, active ? PURPLE : 0xFF414152);
            context.drawText(textRenderer, translatedCategory(category), panelX + 30, cy, active ? 0xFFFFFFFF : 0xFF9696A9, false);
            cy += 34;
        }
        int optimizeY = panelY + panelH - 43;
        context.fill(panelX + 19, optimizeY, panelX + 140, optimizeY + 25, optimized ? 0xFF245E55 : 0xFF303047);
        String optimizeLabel = optimized
                ? (pt() ? "JOGO OTIMIZADO" : "GAME OPTIMIZED")
                : (pt() ? "OTIMIZAR JOGO" : "OPTIMIZE GAME");
        int optimizeWidth = textRenderer.getWidth(optimizeLabel);
        context.drawText(textRenderer, optimizeLabel, panelX + 79 - optimizeWidth / 2, optimizeY + 8,
                optimized ? 0xFFB5FFE6 : 0xFFB9B9D0, true);
        int listX = panelX + 164;
        int listY = panelY + 78;
        int listW = panelW - 178;
        context.fill(listX, listY, listX + listW, panelY + panelH - 14, 0x99191925);
        int rowY = listY + 8 - scroll;
        for (GojoModule module : cachedVisible) {
            if (rowY + 54 > listY && rowY < panelY + panelH - 14) {
                context.fill(listX + 8, rowY, listX + listW - 8, rowY + 52, 0xFF222230);
                context.drawText(textRenderer, translatedName(module.name()), listX + 18, rowY + 6, 0xFFE8E8F4, false);
                String description = translatedDescription(module.description());
                if (description.length() > 53) description = description.substring(0, 50) + "...";
                context.drawText(textRenderer, description, listX + 18, rowY + 19, 0xFF85859B, false);
                if (module.name().equals("Speed") || module.name().equals("Aimbot") || module.name().equals("Auto Click") || module.name().equals("Hitbox") || module.name().equals("Mob KillAura") || module.name().equals("KillAura") || module.name().equals("Fly")) {
                    int sx = listX + 18;
                    int sliderW = Math.min(125, listW - 110);
                    int sy = rowY + 42;
                    int max = module.maxValue();
                    context.fill(sx, sy, sx + sliderW, sy + 5, 0xFF414152);
                    context.fill(sx, sy, sx + sliderW * module.value() / max, sy + 5, PURPLE);
                    String setting = module.name().equals("Aimbot")
                            ? "CAMPO DE VISÃO " + module.value() + "°"
                            : (module.name().equals("Auto Click") || module.name().equals("Mob KillAura") || module.name().equals("KillAura")) ? "CLIQUES/S " + module.value()
                            : module.name().equals("Hitbox") ? "TAMANHO " + module.value() + "/100"
                            : module.name().equals("Fly") ? "VELOCIDADE DO VOO " + String.format(Locale.ROOT, "%.2f", 0.05 + module.value() * 0.015)
                            : "VELOCIDADE " + module.value() + "/100";
                    context.drawText(textRenderer, setting, sx + sliderW + 8, rowY + 38, 0xFFB9B9D0, false);
                }
                int bx = listX + listW - 66;
                int color = module.implemented() ? (module.enabled() ? 0xFF245E83 : 0xFF343444) : 0xFF292932;
                context.fill(bx, rowY + 8, bx + 48, rowY + 28, color);
                String status = !module.implemented() ? (pt() ? "EM BREVE" : "SOON") : module.enabled() ? (pt() ? "LIGADO" : "ON") : (pt() ? "DESLIGADO" : "OFF");
                int sw = textRenderer.getWidth(status);
                context.drawText(textRenderer, status, bx + (48 - sw) / 2, rowY + 14, module.enabled() ? 0xFF9EDBFF : 0xFFAAAAAF, false);
            }
            rowY += ROW_STEP;
        }
        context.drawText(textRenderer, "" + cachedVisible.size() + (pt() ? " MÓDULOS" : " MODULES"), listX + 10, panelY + panelH - 29, 0xFF77778A, false);
        super.render(context, mouseX, mouseY, delta);
    }

    private static boolean pt() { return ClientLanguage.isPortuguese(); }

    private static String translatedName(String name) {
        if (!pt()) return name;
        return switch (name) {
            case "Reach Indicator" -> "Indicador de Alcance";
            case "Target Indicator" -> "Indicador de Alvo";
            case "Combat Info" -> "Informações de Combate";
            case "KillAura" -> "Aura de Ataque";
            case "Mob KillAura" -> "Aura de Ataque em Criaturas";
            case "Hitbox" -> "Caixa de Colisão";
            case "Auto Click" -> "Clique Automático";
            case "Aimbot" -> "Mira Automática";
            case "Combat Visuals" -> "Visuais de Combate";
            case "Auto Sprint" -> "Corrida Automática";
            case "Auto Eat" -> "Comer Automaticamente";
            case "NoFall" -> "Sem Dano de Queda";
            case "NoSlow" -> "Sem Lentidão";
            case "Step Assist" -> "Assistente de Degraus";
            case "Movement Control" -> "Controle de Movimento";
            case "Speed" -> "Velocidade";
            case "Fly" -> "Voar";
            case "Scaffold" -> "Construção Automática";
            case "Fullbright" -> "Brilho Máximo";
            case "Entity ESP" -> "ESP de Entidades";
            case "Player ESP" -> "ESP de Jogadores";
            case "Player Box ESP" -> "Caixa ESP de Jogadores";
            case "X-Ray" -> "Visão de Minérios";
            case "Chest ESP" -> "ESP de Baús";
            case "Furnace ESP" -> "ESP de Fornalhas";
            case "Tracers" -> "Rastreadores";
            case "Nametags" -> "Nomes Aprimorados";
            case "Block Indicator" -> "Indicador de Bloco";
            case "Lighting" -> "Iluminação";
            case "Auto Tool" -> "Ferramenta Automática";
            case "Auto Respawn" -> "Ressurgimento Automático";
            case "Coordinates" -> "Coordenadas";
            case "Player Info" -> "Informações do Jogador";
            case "Inventory Manager" -> "Gerenciador de Inventário";
            case "Block Info" -> "Informações do Bloco";
            case "Structure Finder" -> "Localizador de Estruturas";
            case "Entity Viewer" -> "Visualizador de Entidades";
            case "Environment Info" -> "Informações do Ambiente";
            case "FPS" -> "Quadros por Segundo";
            case "Ping" -> "Latência";
            case "HUD Coordinates" -> "Coordenadas na Tela";
            case "HUD Speed" -> "Velocidade na Tela";
            case "Durability" -> "Durabilidade";
            case "Clock" -> "Relógio";
            case "Server Info" -> "Informações do Servidor";
            case "Connection Monitor" -> "Monitor de Conexão";
            case "Server Compatibility" -> "Compatibilidade com Servidor";
            default -> name;
        };
    }

    private static String translatedDescription(String description) {
        if (!pt()) return description;
        return switch (description) {
            case "Displays distance to the crosshair target." -> "Mostra a distância até o alvo na mira.";
            case "Shows the entity currently under the crosshair." -> "Mostra a entidade que está na mira.";
            case "Displays compact combat context in the HUD." -> "Mostra informações resumidas de combate na tela.";
            case "Automatically attacks visible players in a short range." -> "Ataca automaticamente jogadores visíveis a curta distância.";
            case "Automatically attacks visible hostile and passive mobs in range." -> "Ataca automaticamente criaturas hostis e passivas ao alcance.";
            case "Expands nearby player and mob hitboxes (client-side targeting)." -> "Aumenta as caixas de colisão de jogadores e criaturas próximas.";
            case "Repeats the attack input while the left mouse button is held (1-20 CPS)." -> "Repete o ataque enquanto o botão esquerdo estiver pressionado (1–20 cliques/s).";
            case "Smoothly aims at visible players inside the adjustable FOV." -> "Ajusta suavemente a mira para jogadores visíveis dentro do campo de visão.";
            case "Shows a target card with health and distance." -> "Mostra um painel do alvo com vida e distância.";
            case "Sprints while moving forward, when allowed by the player state." -> "Corre automaticamente ao avançar, quando possível.";
            case "Uses food from your hotbar when hunger is low." -> "Consome comida da barra rápida quando a fome está baixa.";
            case "Resets local fall distance while enabled." -> "Zera a distância de queda local enquanto ativado.";
            case "Applies a local movement compensation while using items; server rules still apply." -> "Compensa a lentidão ao usar itens; as regras do servidor continuam valendo.";
            case "Automatically jumps when moving into a low obstacle." -> "Pula automaticamente ao encontrar um obstáculo baixo.";
            case "Movement status display and local controls." -> "Exibe o estado do movimento e controles locais.";
            case "Adjusts horizontal movement speed (1-100)." -> "Ajusta a velocidade horizontal (1–100).";
            case "Places blocks beneath you while bridging, when a valid support face exists." -> "Coloca blocos abaixo de você ao construir pontes, quando há uma superfície válida.";
            case "Sets client gamma to maximum brightness." -> "Define o brilho do cliente no máximo.";
            case "Highlights nearby living entities through walls." -> "Destaca criaturas vivas próximas, inclusive atrás de blocos.";
            case "Highlights nearby player entities through walls." -> "Destaca jogadores próximos, inclusive atrás de blocos.";
            case "Draws a 3D outline around nearby player hitboxes." -> "Desenha um contorno 3D nas caixas de colisão de jogadores próximos.";
            case "Highlights valuable ore blocks in loaded chunks with an on-screen toggle." -> "Destaca minérios valiosos em áreas carregadas.";
            case "Shows nearby chests, trapped chests, ender chests and barrels with distance and coordinates." -> "Mostra baús próximos, com distância e coordenadas.";
            case "Shows nearby furnaces, blast furnaces and smokers with distance and coordinates." -> "Mostra fornalhas próximas, com distância e coordenadas.";
            case "Draws on-screen direction lines to nearby living entities." -> "Desenha linhas na tela apontando para criaturas próximas.";
            case "Enhanced entity labels for permitted/private worlds." -> "Nomes aprimorados de entidades em mundos permitidos ou privados.";
            case "Displays the block under the crosshair." -> "Mostra o bloco que está na mira.";
            case "Client-side lighting presentation controls." -> "Controles visuais de iluminação do cliente.";
            case "Selects the fastest tool in your hotbar for the targeted block." -> "Seleciona a ferramenta mais rápida da barra para o bloco mirado.";
            case "Automatically respawns after death." -> "Ressurge automaticamente após morrer.";
            case "Displays current XYZ coordinates." -> "Mostra as coordenadas XYZ atuais.";
            case "Displays player and dimension information." -> "Mostra informações do jogador e da dimensão.";
            case "Refills the selected hotbar slot from the main inventory when it becomes empty." -> "Reabastece o espaço selecionado da barra rápida quando fica vazio.";
            case "Displays targeted block state and position." -> "Mostra o estado e a posição do bloco mirado.";
            case "Visual structure discovery; no server scanning." -> "Descoberta visual de estruturas, sem varredura do servidor.";
            case "Displays nearby entity counts." -> "Mostra a quantidade de entidades próximas.";
            case "Displays biome and dimension context." -> "Mostra informações do bioma e da dimensão.";
            case "Displays current frames per second." -> "Mostra os quadros por segundo atuais.";
            case "Displays connection latency when available." -> "Mostra a latência da conexão, quando disponível.";
            case "Coordinates overlay." -> "Exibe as coordenadas na tela.";
            case "Movement speed overlay." -> "Exibe a velocidade de movimento na tela.";
            case "Displays equipped item durability." -> "Mostra a durabilidade do item equipado.";
            case "Displays local system time." -> "Mostra o horário local do sistema.";
            case "Displays server and connection details." -> "Mostra detalhes do servidor e da conexão.";
            case "Shows whether the client is in singleplayer or multiplayer and displays current ping." -> "Mostra se está no modo individual ou multijogador e exibe a latência atual.";
            case "Disables automated combat and movement modules while connected to a multiplayer server." -> "Desativa módulos automáticos de combate e movimento ao entrar em um servidor multijogador.";
            default -> description;
        };
    }

    private static String translatedCategory(ModuleCategory category) {
        if (!pt()) return switch (category) {
            case COMBAT -> "COMBAT";
            case MOVEMENT -> "MOVEMENT";
            case RENDER -> "RENDER";
            case PLAYER -> "PLAYER";
            case WORLD -> "WORLD";
            case HUD -> "HUD";
        };
        return switch (category) {
            case COMBAT -> "COMBATE";
            case MOVEMENT -> "MOVIMENTO";
            case RENDER -> "VISUAL";
            case PLAYER -> "JOGADOR";
            case WORLD -> "MUNDO";
            case HUD -> "HUD";
        };
    }

    private void refreshFiltered() {
        String query = search == null ? "" : search.getText().toLowerCase(Locale.ROOT);
        if (query.equals(cachedQuery)) return;
        cachedQuery = query;
        List<GojoModule> result = new ArrayList<>();
        for (GojoModule module : ModuleManager.all()) {
            if (module.category() == selected && (query.isBlank()
                    || module.name().toLowerCase(Locale.ROOT).contains(query)
                    || module.description().toLowerCase(Locale.ROOT).contains(query)
                    || translatedName(module.name()).toLowerCase(Locale.ROOT).contains(query)
                    || translatedDescription(module.description()).toLowerCase(Locale.ROOT).contains(query))) {
                result.add(module);
            }
        }
        cachedVisible = List.copyOf(result);
        clampScroll();
    }

    private void clampScroll() {
        int viewport = Math.max(0, panelH - 112);
        scroll = Math.max(0, Math.min(Math.max(0, cachedVisible.size() * ROW_STEP - viewport), scroll));
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) {
            if (mouseX >= panelX + panelW - 104 && mouseX <= panelX + panelW - 18
                    && mouseY >= panelY + 42 && mouseY <= panelY + 65) {
                ClientLanguage.toggle();
                cachedQuery = null;
                refreshFiltered();
                return true;
            }
            int catY = panelY + 94;
            for (ModuleCategory category : ModuleCategory.values()) {
                if (mouseX >= panelX + 15 && mouseX <= panelX + 146 && mouseY >= catY - 7 && mouseY <= catY + 19) {
                    selected = category; scroll = 0; cachedQuery = null; refreshFiltered(); return true;
                }
                catY += 34;
            }
            int listX = panelX + 164;
            int listW = panelW - 178;
            int rowY = panelY + 86 - scroll;
            for (GojoModule module : cachedVisible) {
                if (module.name().equals("Speed") || module.name().equals("Aimbot") || module.name().equals("Auto Click") || module.name().equals("Hitbox") || module.name().equals("Mob KillAura") || module.name().equals("KillAura") || module.name().equals("Fly")) {
                    int sx = listX + 18;
                    int sliderW = Math.min(125, listW - 110);
                    if (mouseX >= sx && mouseX <= sx + sliderW && mouseY >= rowY + 39 && mouseY <= rowY + 51) {
                        int next = (int) Math.round((mouseX - sx) * module.maxValue() / sliderW);
                        module.setValue(next);
                        ConfigManager.save(ModuleManager.all());
                        return true;
                    }
                }
                if (mouseX >= listX + 8 && mouseX <= panelX + panelW - 14 && mouseY >= rowY && mouseY <= rowY + 52) {
                    module.toggle();
                    ConfigManager.save(ModuleManager.all());
                    return true;
                }
                rowY += ROW_STEP;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) (verticalAmount * 22);
        clampScroll();
        return true;
    }

    @Override public boolean shouldPause() { return false; }
}
