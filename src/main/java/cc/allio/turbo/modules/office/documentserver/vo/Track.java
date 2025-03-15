package cc.allio.turbo.modules.office.documentserver.vo;

import cc.allio.uno.core.api.Copyable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Track implements Copyable<Track> {
    private String filetype;
    private String url;
    private String key;
    private String changesurl;
    private History history;
    private String token;
    private Integer forcesavetype;
    private Integer status;
    private List<String> users;
    private List<Action> actions;
    private String userdata;
    private String lastsave;
    private Boolean notmodified;

    @Override
    public Track copy() {
        Track track = new Track();
        track.setFiletype(filetype);
        track.setUrl(url);
        track.setKey(key);
        track.setChangesurl(changesurl);
        track.setHistory(history);
        track.setToken(token);
        track.setForcesavetype(forcesavetype);
        track.setStatus(status);
        track.setUsers(users);
        track.setActions(actions);
        track.setUserdata(userdata);
        track.setLastsave(lastsave);
        track.setNotmodified(notmodified);
        return track;
    }
}
