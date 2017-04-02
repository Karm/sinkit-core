package biz.karms.sinkit.ioc;

import java.io.Serializable;

/**
 * @author Tomas Kozel
 */
public class IoCSourceId implements Serializable {

    private static final long serialVersionUID = 2510134193901542038L;

    private String value;
    private IoCSourceIdType type;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public IoCSourceIdType getType() {
        return type;
    }

    public void setType(IoCSourceIdType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "IoCSourceId{" +
                "value='" + value + '\'' +
                ", type=" + type +
                '}';
    }
}
