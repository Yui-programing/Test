package com.musicfinder.server.handler;

import java.util.Optional;
import java.util.List;

import com.musicfinder.server.api.GoogleSuggestApi;
import com.musicfinder.server.api.YtDlpApi;
import com.musicfinder.server.service.CacheService;
import com.musicfinder.shared.request.payload.SongPayload;
import com.musicfinder.shared.response.payload.SongData;
import com.musicfinder.shared.response.payload.SearchArtistData;

import com.musicfinder.server.service.ArtistService;
import com.musicfinder.server.service.AuthService;
import com.musicfinder.server.service.HistoryService;
import com.musicfinder.server.service.PlayListService;
import com.musicfinder.server.service.SearchService;
import com.musicfinder.server.service.StatisticsService;
import com.musicfinder.shared.enums.Status;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.Add_RemoveToPlaylistPayload;
import com.musicfinder.shared.request.payload.ArtistPayload;
import com.musicfinder.shared.request.payload.ChangePasswordProfilePayload;
import com.musicfinder.shared.request.payload.Create_UpdateToPlaylistPayload;
import com.musicfinder.shared.request.payload.EditProfilePayload;
import com.musicfinder.shared.request.payload.SearchPayload;
import com.musicfinder.shared.request.payload.LoginPayload;
import com.musicfinder.shared.request.payload.RegisterPayload;
import com.musicfinder.shared.request.payload.RemoveRowSearchHistory;
import com.musicfinder.shared.request.payload.Remove_OpenPlaylistPayload;
import com.musicfinder.shared.request.payload.StatisticsPayload;
import com.musicfinder.shared.response.Response;
import com.musicfinder.shared.response.payload.SearchSongData;
import com.musicfinder.shared.response.payload.UserData;
import com.musicfinder.shared.response.payload.ViewArtistDetailData;
import com.musicfinder.shared.response.payload.ViewArtistSongsData;

/* NOTE CHO CLIENT
 * Response TCP: {requestId, action, status, message, data}; đọc data khi status = SUCCESS.
 * SEARCH_SONG   {keyword} -> data = {songs: SongData[], suggestions: SongData[]}.
 * SUGGEST       {keyword} -> data = string[] (gợi ý từ khóa, không có songID).
 * VIEW_SONG_DETAIL {songID} -> data = SongData; đưa data.audio_url vào player HTTP.
 * SEARCH_ARTIST {keyword} -> data = {artists: ArtistData[], suggestions: ArtistData[]}.
 * VIEW_ARTIST_DETAIL {artistID} -> data.artist; VIEW_ARTIST_SONGS {artistID} -> data.songs.
 * Giữ đúng tên trường songID, artistID, durations_seconds, audio_url; xử lý giá trị null.
 * TCP 12345 chỉ trả JSON; byte MP3 lấy từ link HTTP 8088. Gọi chi tiết trước mỗi lần phát.
 */

public class RequestDispatcher {

    // Service tạo 1 lần, dùng chung cho mọi Client (service không giữ dữ liệu riêng
    // của request)
    private final StatisticsService statisticsService = new StatisticsService();
    private final ArtistService artistService;
    private final HistoryService historyService = new HistoryService();
    private final AuthService authService = new AuthService();
    private final PlayListService playListService;
    private final SearchService searchService;
    private final CacheService cacheService = new CacheService();

    public RequestDispatcher() {
        this(new YtDlpApi());
    }

    public RequestDispatcher(YtDlpApi api) {
        searchService = new SearchService(api, cacheService);
        artistService = new ArtistService(cacheService, api, searchService);
        playListService = new PlayListService(searchService);
    }

    public Response handle(String json) {
        return handle(json, null);
    }

    // Nhận 1 dòng JSON từ client, luôn trả về 1 Response (không ném lỗi ra ngoài)
    // userId = id của user đã đăng nhập trên kết nối này, null nếu chưa đăng nhập
    public Response handle(String json, Long userId) {
        Request request;
        try {
            request = Request.fromJson(json);
        } catch (IllegalArgumentException e) {
            return Response.invalid("Dữ liệu gửi lên không phải JSON hợp lệ");
        }

        if (request == null || request.getAction() == null) {
            return Response.invalid("Thiếu hoặc sai action");
        }

        try {
            switch (request.getAction()) {
                case PING:
                    return Response.success(request, "PONG");
                case LOGOUT:
                    return Response.success(request, null);

                // ======= AUTH ========
                case REGISTER: {
                    RegisterPayload payload = request.dataAs(RegisterPayload.class);
                    UserData data = authService.register(payload);
                    return Response.success(request, data);
                }
                case LOGIN: {
                    LoginPayload payload = request.dataAs(LoginPayload.class);
                    UserData data = authService.login(payload);
                    return Response.success(request, data);
                }
                case GET_PROFILE: {
                    String username = (request.getData() != null && request.getData().has("username"))
                            ? request.getData().path("username").asText("")
                            : "";
                    UserData data;
                    if (!username.isBlank()) {
                        data = authService.getProfile(username);
                    } else if (userId != null) {
                        data = authService.getProfileById(userId);
                    } else {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    return Response.success(request, data);
                }
                case UPDATE_PROFILE: {
                    String username = (request.getData() != null && request.getData().has("username"))
                            ? request.getData().path("username").asText("")
                            : "";
                    EditProfilePayload payload = request.dataAs(EditProfilePayload.class);
                    UserData data;
                    if (!username.isBlank()) {
                        data = authService.updateProfile(username, payload);
                    } else if (userId != null) {
                        data = authService.updateProfileById(userId, payload);
                    } else {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    return Response.success(request, data);
                }
                case CHANGE_PASSWORD: {
                    String username = (request.getData() != null && request.getData().has("username"))
                            ? request.getData().path("username").asText("")
                            : "";
                    ChangePasswordProfilePayload payload = request.dataAs(ChangePasswordProfilePayload.class);
                    if (!username.isBlank()) {
                        authService.changePassword(username, payload);
                    } else if (userId != null) {
                        authService.changePasswordById(userId, payload);
                    } else {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    return Response.success(request, "Đổi mật khẩu thành công");
                }

                // ===== Thống kê =====
                case GET_TOP_SONGS:
                    return Response.success(request, statisticsService.getTopSongs(readLimit(request)));

                case GET_TOP_ARTISTS:
                    return Response.success(request, statisticsService.getTopArtists(readLimit(request)));

                // ===== Ca sĩ =====
                case VIEW_ARTIST_DETAIL: {
                    ArtistPayload payload = request.dataAs(ArtistPayload.class);
                    Optional<ViewArtistDetailData> result = artistService.viewArtistDetail(payload.artistID());
                    if (result.isEmpty()) {
                        return Response.error(request, Status.NOT_FOUND, "Không tìm thấy ca sĩ");
                    }
                    return Response.success(request, result.get());
                }

                case VIEW_ARTIST_SONGS: {
                    ArtistPayload payload = request.dataAs(ArtistPayload.class);
                    Optional<ViewArtistSongsData> result = artistService.viewArtistSongs(payload.artistID());
                    if (result.isEmpty()) {
                        return Response.error(request, Status.NOT_FOUND, "Không tìm thấy ca sĩ");
                    }
                    return Response.success(request, result.get());
                }

                // ===== Lịch sử tìm kiếm (phải đăng nhập) =====
                case GET_HISTORY:
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    return Response.success(request, historyService.getHistory(userId));

                case REMOVE_ROW_HISTORY: {
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    RemoveRowSearchHistory payload = request.dataAs(RemoveRowSearchHistory.class);
                    boolean removed = historyService.removeRow(userId, payload.historyID());
                    if (!removed) {
                        return Response.error(request, Status.NOT_FOUND, "Không tìm thấy dòng lịch sử");
                    }
                    return Response.success(request, null);
                }

                case CLEAR_HISTORY:
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    historyService.clearHistory(userId);
                    return Response.success(request, null);

                // ========================SEARCH=========================
                case SEARCH_SONG: {
                    SearchPayload payload = request.dataAs(SearchPayload.class);
                    SearchSongData data = searchService.searchSongs(payload.keyword(), userId);
                    return Response.success(request, data);
                }

                case SEARCH_ARTIST: {
                    SearchPayload payload = request.dataAs(SearchPayload.class);
                    SearchArtistData data = artistService.searchArtist(payload.keyword(), userId);
                    return Response.success(request, data);
                }
                case VIEW_SONG_DETAIL: {
                    SongPayload payload = request.dataAs(SongPayload.class);
                    SongData data = searchService.detail(payload.songID(), payload.externalId());
                    return Response.success(request, data);
                }
                case SUGGEST: {
                    SearchPayload payload = request.dataAs(SearchPayload.class);
                    List<String> suggestions = GoogleSuggestApi.suggest(payload.keyword());
                    return Response.success(request, suggestions);
                }

                // ===============PLAYLIST========================
                case CREATE_PLAYLIST: {
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    Create_UpdateToPlaylistPayload payload = request.dataAs(Create_UpdateToPlaylistPayload.class);
                    return Response.success(request, playListService.createPlaylist(userId, payload));
                }

                case GET_PLAYLISTS:
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    return Response.success(request, playListService.getPlaylists(userId));

                case EDIT_FROM_PLAYLIST: {
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    Create_UpdateToPlaylistPayload payload = request.dataAs(Create_UpdateToPlaylistPayload.class);
                    boolean ok = playListService.editPlaylist(userId, payload);
                    return ok ? Response.success(request, "Cập nhật playlist thành công")
                            : Response.error(request, Status.ERROR, "Cập nhật thất bại");
                }

                case DELETE_PLAYLIST: {
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    Remove_OpenPlaylistPayload payload = request.dataAs(Remove_OpenPlaylistPayload.class);
                    boolean ok = playListService.deletePlaylist(userId, payload);
                    return ok ? Response.success(request, "Đã xóa playlist")
                            : Response.error(request, Status.ERROR, "Xóa playlist thất bại");
                }

                case GET_PLAYLIST_SONGS: {
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    Remove_OpenPlaylistPayload payload = request.dataAs(Remove_OpenPlaylistPayload.class);
                    return Response.success(request, playListService.getPlaylistSongs(userId, payload));
                }

                case ADD_TO_PLAYLIST: {
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    Add_RemoveToPlaylistPayload payload = request.dataAs(Add_RemoveToPlaylistPayload.class);
                    boolean added = playListService.addSongToPlaylist(userId, payload);
                    return added ? Response.success(request, "Đã thêm bài hát vào playlist")
                            : Response.error(request, Status.ERROR, "Bài hát đã có trong playlist hoặc thêm thất bại");
                }

                case REMOVE_FROM_PLAYLIST: {
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    Add_RemoveToPlaylistPayload payload = request.dataAs(Add_RemoveToPlaylistPayload.class);
                    boolean removed = playListService.removeSongFromPlaylist(userId, payload);

                    return removed ? Response.success(request, "Đã xoá bài hát trong playlist")
                            : Response.error(request, Status.ERROR, "Xoá thất bại");
                }

                case GET_FAVORITES:
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    return Response.success(request, playListService.getFavorites(userId));

                case ADD_FAVORITE: {
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    Add_RemoveToPlaylistPayload payload = request.dataAs(Add_RemoveToPlaylistPayload.class);
                    boolean added = playListService.addFavorite(userId, payload);

                    return added ? Response.success(request, "Đã thêm bài hát vào danh sách yêu thích")
                            : Response.error(request, Status.ERROR, "Thêm thất bại");
                }

                case REMOVE_FAVORITE: {
                    if (userId == null) {
                        return Response.error(request, Status.UNAUTHORIZED, "Bạn chưa đăng nhập");
                    }
                    Add_RemoveToPlaylistPayload payload = request.dataAs(Add_RemoveToPlaylistPayload.class);
                    boolean removed = playListService.removeFavorite(userId, payload);
                    return removed ? Response.success(request, "Đã xoá bài hát thành công")
                            : Response.error(request, Status.ERROR, "Xoá thất bại");
                }

                // Các action khác sẽ thêm dần ở đây:
                // case SEARCH_SONG: return searchHandler.searchSong(request);

                default:
                    return Response.error(request, Status.INVALID_REQUEST,
                            "Chưa hỗ trợ action: " + request.getAction());
            }
        } catch (IllegalArgumentException e) {
            return Response.error(request, Status.INVALID_REQUEST, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(request, Status.SERVER_ERROR, "Lỗi xử lý phía server");
        }
    }

    private int readLimit(Request request) {
        if (request.getData() == null || request.getData().isNull()) {
            return 0;
        }
        return request.dataAs(StatisticsPayload.class).limit();
    }
}