package org.teinelund.tools.c2plantuml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class ApplicationTest {

    private Application sut = null;

    @BeforeEach
    void init(TestInfo testInfo) {
        this.sut = new Application();
    }

    @Test
    void executeWhereArgsContainsVersionOption() {
        // Initialize
        String[] args = {"-V"};
        // Test
        // this.sut.execute(args, null);
        // Verify
    }
}
