package ru.ctd.config;

import lombok.Data;

@Data
public class ApplicationProperties {
    public String version = "1.0.1";

    public boolean soundOn = true;
    public boolean musicOn = true;
    public int soundVolumePercent = 70;
    public int musicVolumePercent = 85;
}
