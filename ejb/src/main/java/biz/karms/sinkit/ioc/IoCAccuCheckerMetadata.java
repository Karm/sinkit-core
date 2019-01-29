package biz.karms.sinkit.ioc;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

/**
 * @author Krystof Kolar
 */
@Getter
@Setter
public class IoCAccuCheckerMetadata {

    private static final long serialVersionUID = -8503565375996995715L;

    private Date timestamp;
    private String content;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
