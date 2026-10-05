package model.grado;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GradoDisplayDTO { //TODO: CAMBIAR ATRIBUTOS DE CARA A FUTURO
    private String id; 
    private String nombre;
    private String facultad;
    private String estado;
}