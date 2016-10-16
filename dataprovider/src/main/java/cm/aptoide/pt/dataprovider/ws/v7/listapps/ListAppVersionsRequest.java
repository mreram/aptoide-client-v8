/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 17/08/2016.
 */

package cm.aptoide.pt.dataprovider.ws.v7.listapps;

import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.dataprovider.repository.IdsRepository;
import cm.aptoide.pt.dataprovider.ws.Api;
import cm.aptoide.pt.dataprovider.ws.BaseBodyDecorator;
import cm.aptoide.pt.dataprovider.ws.v7.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v7.Endless;
import cm.aptoide.pt.dataprovider.ws.v7.V7;
import cm.aptoide.pt.model.v7.listapp.ListAppVersions;
import cm.aptoide.pt.networkclient.WebService;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import rx.Observable;

/**
 * Created by neuro on 22-04-2016.
 */
@Data @EqualsAndHashCode(callSuper = true) public class ListAppVersionsRequest
    extends V7<ListAppVersions, ListAppVersionsRequest.Body> {

  private static final Integer MAX_LIMIT = 10;

  private ListAppVersionsRequest(Body body, String baseHost, String email) {
    super(body, WebService.getDefaultConverter(), baseHost, email);
  }

  public static ListAppVersionsRequest of(String accessToken, String email) {
    BaseBodyDecorator decorator = new BaseBodyDecorator(
        new IdsRepository(SecurePreferencesImplementation.getInstance(),
            DataProvider.getContext()));
    Body body = new Body();
    body.setLimit(MAX_LIMIT);
    return new ListAppVersionsRequest((Body) decorator.decorate(body, accessToken), BASE_HOST,
        email);
  }

  public static ListAppVersionsRequest of(int limit, int offset, String accessToken, String email) {
    BaseBodyDecorator decorator = new BaseBodyDecorator(
        new IdsRepository(SecurePreferencesImplementation.getInstance(),
            DataProvider.getContext()));
    Body body = new Body();
    body.setLimit(limit);
    body.setOffset(offset);
    return new ListAppVersionsRequest((Body) decorator.decorate(body, accessToken), BASE_HOST,
        email);
  }

  public static ListAppVersionsRequest of(String packageName, String accessToken, String email) {
    BaseBodyDecorator decorator = new BaseBodyDecorator(
        new IdsRepository(SecurePreferencesImplementation.getInstance(),
            DataProvider.getContext()));
    Body body = new Body(packageName);
    body.setLimit(MAX_LIMIT);
    return new ListAppVersionsRequest((Body) decorator.decorate(body, accessToken), BASE_HOST,
        email);
  }

  public static ListAppVersionsRequest of(String packageName, int limit, int offset,
      String accessToken, String email) {
    BaseBodyDecorator decorator = new BaseBodyDecorator(
        new IdsRepository(SecurePreferencesImplementation.getInstance(),
            DataProvider.getContext()));
    Body body = new Body(packageName);
    body.setLimit(limit);
    body.setOffset(offset);
    return new ListAppVersionsRequest((Body) decorator.decorate(body, accessToken), BASE_HOST,
        email);
  }

  @Override protected Observable<ListAppVersions> loadDataFromNetwork(Interfaces interfaces,
      boolean bypassCache) {
    return interfaces.listAppVersions(body, bypassCache);
  }

  @Data @Accessors(chain = false) @EqualsAndHashCode(callSuper = true) public static class Body
      extends BaseBody implements Endless {

    private Integer apkId;
    private String apkMd5sum;
    private Integer appId;
    private String lang = Api.LANG;
    @Setter @Getter private Integer limit;
    @Setter @Getter private int offset;
    private Integer packageId;
    private String packageName;
    private String q = Api.Q;
    private List<Long> storeIds;
    private List<String> storeNames;

    public Body() {
    }

    public Body(String packageName) {
      this.packageName = packageName;
    }
  }
}
