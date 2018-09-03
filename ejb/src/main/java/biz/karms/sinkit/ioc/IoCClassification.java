package biz.karms.sinkit.ioc;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class IoCClassification implements Serializable {

    private static final long serialVersionUID = -5212807838160280916L;

    private String type;
    private String taxonomy;
    private String identifier;

    @Override
    public String toString() {
        return "IoCClassification{" +
                "type='" + type + '\'' +
                ", taxonomy='" + taxonomy + '\'' +
                ", identifier='" + identifier + '\'' +
                '}';
    }
}
