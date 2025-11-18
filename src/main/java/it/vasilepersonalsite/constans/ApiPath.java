package it.vasilepersonalsite.constans;

import lombok.Data;

@Data
public class ApiPath {

    // Path base per tutte le API REST
    public static final String BASE_PATH = "luigi/vasile/personal/api";

    // Path per i progetti GitHub
    public static final String STACK_PATH = "stack";

    // Path per il ccontroller lezioni
    public static final String LESSON_PATH = "lezioni";

}

