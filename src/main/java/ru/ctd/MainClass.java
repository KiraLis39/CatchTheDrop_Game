package ru.ctd;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.ctd.game.Game;

import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

@Slf4j
@Getter
@Setter
public class MainClass {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (Exception e) {
            System.err.println("Couldn't get specified look and feel, for some reason.");
        }

        new Game();
    }
}
