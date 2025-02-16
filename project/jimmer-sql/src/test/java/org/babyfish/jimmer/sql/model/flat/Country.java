package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
@DatabaseValidationIgnore
public interface Country {

    @Id
    @Column(name = "code")
    long id();

    String countryName();

    @OneToMany(mappedBy = "country")
    List<Province> provinces();
}
