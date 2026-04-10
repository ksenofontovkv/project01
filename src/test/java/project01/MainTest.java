package project01;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

public class MainTest {

    @Test
    public void mainTest() throws NoSuchFieldException, SecurityException, IOException {
        Logger mockLogger = Mockito.mock(Logger.class);
        ReflectionTestUtils.setField(Main.class, "logger", mockLogger);
        
        Main.main(new String[] {"lng_tst.txt"});
        
        verify(mockLogger, times(2)).log(eq(Level.SEVERE), anyString());
        Assert.isTrue(new String(Files.readAllBytes(new File("lng_out.txt").toPath()), StandardCharsets.UTF_8).contains(
                "Количество групп с более чем одним элементом: 1"), 
                "Количество найденных групп должно быть 1");
        ;
    }
}
