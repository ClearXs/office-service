package cc.allio.turbo.modules.office.constant;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum WebhookType {

    SAVE("save", "save document callback"),
    EDIT("edit", "edit document callback"),
    CORRUPTED("corrupted", "corrupt"),
    FORCESAVE("forcesave", "force save");

    @JsonValue
    @EnumValue
    private final String value;
    private final String label;
}
