package com.rpantieri.photoDuplicateFinder.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BiObjectHolder<FIRST, SECOND> {
    private FIRST first;
    private SECOND second;
}
