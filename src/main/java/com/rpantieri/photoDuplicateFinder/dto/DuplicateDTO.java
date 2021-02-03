package com.rpantieri.photoDuplicateFinder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DuplicateDTO {
    public String filePath;
    public boolean keep;
    public Long imageId;
}
