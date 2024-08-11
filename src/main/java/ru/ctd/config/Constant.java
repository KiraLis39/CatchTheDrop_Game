package ru.ctd.config;

import fox.FoxFontBuilder;
import fox.player.FoxPlayer;

import java.awt.Font;

public class Constant {
    public static final FoxPlayer musicPlayer = new FoxPlayer("musicPlayer");
    public static final FoxPlayer soundPlayer = new FoxPlayer("soundPlayer");

    public static final FoxFontBuilder ffb = new FoxFontBuilder();
    public static final Font f0 = ffb.setFoxFont(FoxFontBuilder.FONT.ARIAL, 22, false);
    public static final Font f1 = ffb.setFoxFont(FoxFontBuilder.FONT.ARIAL_NARROW, 22, false);
    public static final Font f2 = ffb.setFoxFont(FoxFontBuilder.FONT.ARIAL_NARROW, 20, false);
}
