package com.qvqw.idp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTests {

    @Test
    void modulesShouldBeWellFormed() {
        ApplicationModules.of(IdpApplication.class).verify();
    }
}
