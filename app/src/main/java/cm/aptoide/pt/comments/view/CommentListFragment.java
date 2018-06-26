package cm.aptoide.pt.comments.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.analytics.AnalyticsManager;
import cm.aptoide.analytics.implementation.navigation.NavigationTracker;
import cm.aptoide.analytics.implementation.navigation.ScreenTagHistory;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.account.AccountAnalytics;
import cm.aptoide.pt.account.view.AccountNavigator;
import cm.aptoide.pt.comments.CommentDialogCallbackContract;
import cm.aptoide.pt.comments.CommentNode;
import cm.aptoide.pt.comments.ComplexComment;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.database.AccessorFactory;
import cm.aptoide.pt.database.realm.Store;
import cm.aptoide.pt.dataprovider.WebService;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.model.v7.BaseV7Response;
import cm.aptoide.pt.dataprovider.model.v7.Comment;
import cm.aptoide.pt.dataprovider.model.v7.ListComments;
import cm.aptoide.pt.dataprovider.model.v7.SetComment;
import cm.aptoide.pt.dataprovider.util.CommentType;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v7.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v7.BaseRequestWithStore;
import cm.aptoide.pt.dataprovider.ws.v7.ListCommentsRequest;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.navigator.ActivityResultNavigator;
import cm.aptoide.pt.preferences.managed.ManagerPreferences;
import cm.aptoide.pt.store.StoreAnalytics;
import cm.aptoide.pt.store.StoreCredentialsProvider;
import cm.aptoide.pt.store.StoreCredentialsProviderImpl;
import cm.aptoide.pt.store.StoreUtils;
import cm.aptoide.pt.util.CommentOperations;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.view.custom.HorizontalDividerItemDecoration;
import cm.aptoide.pt.view.fragment.GridRecyclerSwipeFragment;
import cm.aptoide.pt.view.recycler.EndlessRecyclerOnScrollListener;
import cm.aptoide.pt.view.recycler.displayable.Displayable;
import cm.aptoide.pt.view.recycler.displayable.DisplayableGroup;
import com.jakewharton.rxbinding.view.RxView;
import com.trello.rxlifecycle.android.FragmentEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.functions.Action1;

// TODO: 21/12/2016 refactor and split in multiple classes to list comments
// for each type: store and timeline card
public class CommentListFragment extends GridRecyclerSwipeFragment
    implements CommentDialogCallbackContract {

  //
  // consts
  //
  private static final String COMMENT_TYPE = "comment_type";
  private static final String ELEMENT_ID_AS_LONG = "element_id_as_long";
  private static final String URL_VAL = "url_val";
  private static final String STORE_ANALYTICS_ACTION = "store_analytics_action";
  private static final String STORE_CONTEXT = "store_context";
  @Inject AnalyticsManager analyticsManager;
  @Inject NavigationTracker navigationTracker;
  //
  // vars
  //
  private CommentOperations commentOperations;
  private List<Displayable> displayables;
  private CommentType commentType;
  private String url;
  private List<CommentNode> comments;
  // store comments vars
  private long elementIdAsLong;
  private String storeName;
  //
  // views
  //
  private FloatingActionButton floatingActionButton;
  private AptoideAccountManager accountManager;
  private AccountNavigator accountNavigator;
  private BodyInterceptor<BaseBody> bodyDecorator;
  private StoreCredentialsProvider storeCredentialsProvider;
  private OkHttpClient httpClient;
  private Converter.Factory converterFactory;
  private TokenInvalidator tokenInvalidator;
  private SharedPreferences sharedPreferences;
  private String storeAnalyticsAction;
  private StoreAnalytics storeAnalytics;
  private StoreContext storeContext;

  public static Fragment newInstanceUrl(CommentType commentType, String url,
      String storeAnalyticsAction, StoreContext storeContext) {
    Bundle args = new Bundle();
    args.putString(URL_VAL, url);
    args.putSerializable(STORE_CONTEXT, storeContext);
    args.putString(COMMENT_TYPE, commentType.name());
    args.putString(STORE_ANALYTICS_ACTION, storeAnalyticsAction);
    CommentListFragment fragment = new CommentListFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    //this object is used in loadExtras and loadExtras is called in the super
    getFragmentComponent(savedInstanceState).inject(this);
    AptoideApplication application = (AptoideApplication) getContext().getApplicationContext();
    sharedPreferences = application.getDefaultSharedPreferences();
    tokenInvalidator = application.getTokenInvalidator();
    storeCredentialsProvider = new StoreCredentialsProviderImpl(AccessorFactory.getAccessorFor(
        ((AptoideApplication) getContext().getApplicationContext()
            .getApplicationContext()).getDatabase(), Store.class));
    httpClient = application.getDefaultClient();
    converterFactory = WebService.getDefaultConverter();
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View v = super.onCreateView(inflater, container, savedInstanceState);
    accountManager =
        ((AptoideApplication) getContext().getApplicationContext()).getAccountManager();
    bodyDecorator =
        ((AptoideApplication) getContext().getApplicationContext()).getAccountSettingsBodyInterceptorPoolV7();
    accountNavigator = ((ActivityResultNavigator) getContext()).getAccountNavigator();
    storeAnalytics = new StoreAnalytics(analyticsManager, navigationTracker);
    return v;
  }

  @Override protected boolean displayHomeUpAsEnabled() {
    return true;
  }

  @Override public void setupToolbarDetails(Toolbar toolbar) {
    if (commentType == CommentType.STORE && !TextUtils.isEmpty(storeName)) {
      String title =
          String.format(getString(R.string.commentlist_title_comment_on_store), storeName);
      toolbar.setTitle(title);
    } else {
      toolbar.setTitle(R.string.comments_title_comments);
    }
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_empty, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      getActivity().onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public int getContentViewId() {
    return R.layout.recycler_swipe_fragment_with_toolbar;
  }

  @Override public void loadExtras(Bundle args) {
    super.loadExtras(args);
    storeContext = ((StoreContext) args.getSerializable(STORE_CONTEXT));
    elementIdAsLong = args.getLong(ELEMENT_ID_AS_LONG);
    url = args.getString(URL_VAL);
    commentType = CommentType.valueOf(args.getString(COMMENT_TYPE));
    storeAnalyticsAction = args.getString(STORE_ANALYTICS_ACTION);

    // extracting store data from the URL...
    if (commentType == CommentType.STORE) {

      BaseRequestWithStore.StoreCredentials storeCredentials =
          StoreUtils.getStoreCredentialsFromUrl(url, storeCredentialsProvider);

      if (storeCredentials != null) {
        Long id = storeCredentials.getId();
        if (id != null) {
          elementIdAsLong = id;
        }

        if (!TextUtils.isEmpty(storeCredentials.getName())) {
          storeName = storeCredentials.getName();
        }
      }
    }
  }

  @Override public void bindViews(View view) {
    super.bindViews(view);
    commentOperations = new CommentOperations();
    floatingActionButton = (FloatingActionButton) view.findViewById(R.id.fabAdd);
    if (floatingActionButton != null) {
      Drawable drawable;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        drawable = getContext().getDrawable(R.drawable.forma_1);
      } else {
        drawable = getActivity().getResources()
            .getDrawable(R.drawable.forma_1);
      }
      floatingActionButton.setImageDrawable(drawable);
      floatingActionButton.setVisibility(View.VISIBLE);
    }
    final CrashReport crashReport = CrashReport.getInstance();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    //sending view comment event in case this fragment is opened from a store
    if (commentType == CommentType.STORE) {
      storeAnalytics.sendStoreInteractEvent(storeAnalyticsAction, "Home",
          storeName == null ? "unknown" : storeName);
    }
  }

  @Override public void load(boolean create, boolean refresh, Bundle savedInstanceState) {
    super.load(create, refresh, savedInstanceState);
    if (create || refresh) {
      refreshData();
    }
  }

  @Override public ScreenTagHistory getHistoryTracker() {
    return ScreenTagHistory.Builder.build(this.getClass()
        .getSimpleName(), "", storeContext != null ? storeContext.name() : null);
  }

  void refreshData() {
    caseListStoreComments(url, StoreUtils.getStoreCredentialsFromUrl(url, storeCredentialsProvider),
        true);
  }

  void caseListStoreComments(String url, BaseRequestWithStore.StoreCredentials storeCredentials,
      boolean refresh) {

    ListCommentsRequest listCommentsRequest =
        ListCommentsRequest.ofStoreAction(url, refresh, storeCredentials, bodyDecorator, httpClient,
            converterFactory, tokenInvalidator,
            ((AptoideApplication) getContext().getApplicationContext()).getDefaultSharedPreferences());

    if (storeCredentials == null || storeCredentials.getId() == null) {
      IllegalStateException illegalStateException =
          new IllegalStateException("Current store credentials does not have a store id");
      CrashReport.getInstance()
          .log(illegalStateException);
      throw illegalStateException;
    }

    final long storeId = storeCredentials.getId() != null ? storeCredentials.getId() : -1;
    final String storeName = storeCredentials.getName();

    Action1<ListComments> listCommentsAction = (listComments -> {
      if (listComments != null
          && listComments.getDataList() != null
          && listComments.getDataList()
          .getList() != null) {
        comments = commentOperations.flattenByDepth(commentOperations.transform(
            listComments.getDataList()
                .getList()));

        ArrayList<Displayable> displayables = new ArrayList<>(comments.size());
        for (CommentNode commentNode : comments) {
          displayables.add(new CommentDisplayable(new ComplexComment(commentNode,
              createNewCommentFragment(storeId, commentNode.getComment()
                  .getId(), storeName)), getFragmentNavigator(),
              ((AptoideApplication) getContext().getApplicationContext()).getFragmentProvider()));
        }

        this.displayables = new ArrayList<>(displayables.size());
        this.displayables.add(new DisplayableGroup(displayables,
            (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE),
            getContext().getResources()));

        addDisplayables(this.displayables);
      }
    });

    getRecyclerView().clearOnScrollListeners();
    EndlessRecyclerOnScrollListener endlessRecyclerOnScrollListener =
        new EndlessRecyclerOnScrollListener(getAdapter(), listCommentsRequest, listCommentsAction,
            err -> err.printStackTrace(), true, false);

    getRecyclerView().addOnScrollListener(endlessRecyclerOnScrollListener);
    endlessRecyclerOnScrollListener.onLoadMore(refresh, refresh);
  }

  private Completable createNewCommentFragment(long storeId, long previousCommentId,
      String storeName) {
    return accountManager.accountStatus()
        .first()
        .toSingle()
        .flatMapCompletable(account -> {
          if (account.isLoggedIn()) {
            // show fragment CommentDialog
            FragmentManager fm = CommentListFragment.this.getActivity()
                .getSupportFragmentManager();
            CommentDialogFragment commentDialogFragment =
                CommentDialogFragment.newInstanceStoreCommentReply(storeId, previousCommentId,
                    storeName);
            commentDialogFragment.setCommentDialogCallbackContract(this);

            return commentDialogFragment.lifecycle()
                .doOnSubscribe(() -> commentDialogFragment.show(fm, "fragment_comment_dialog"))
                .filter(event -> event.equals(FragmentEvent.DESTROY_VIEW))
                .toCompletable();
          }

          return showSignInMessage();
        });
  }

  //
  // Re-Do: 6/1/2017 create new comment different fragment constructions
  //

  //
  // Timeline Articles comments methods
  //

  private Completable showSignInMessage() {
    return Single.just(floatingActionButton)
        .flatMapCompletable(view -> Completable.fromAction(() -> {
          Snackbar.make(view, R.string.you_need_to_be_logged_in, Snackbar.LENGTH_LONG)
              .setAction(R.string.login, snackView -> accountNavigator.navigateToAccountView(
                  AccountAnalytics.AccountOrigins.COMMENT_LIST))
              .show();
        }));
  }

  private Observable<Void> reloadComments() {
    return Observable.fromCallable(() -> {
      ManagerPreferences.setForceServerRefreshFlag(true, sharedPreferences);
      super.reload();
      return null;
    });
  }

  @Override public void setupViews() {
    super.setupViews();
    setupToolbar();

    RxView.clicks(floatingActionButton)
        .flatMap(a -> createNewCommentFragment(elementIdAsLong, storeName).toObservable())
        .compose(bindUntilEvent(LifecycleEvent.DESTROY))
        .subscribe(a -> {
          // no-op
        });
  }

  //
  // Store comments methods
  //

  @Override protected RecyclerView.ItemDecoration getItemDecoration() {
    return new HorizontalDividerItemDecoration(getContext(), 0);
  }

  public Completable createNewCommentFragment(long storeCommentId, String storeName) {
    return accountManager.accountStatus()
        .first()
        .toSingle()
        .flatMapCompletable(account -> {
          if (account.isLoggedIn()) {
            // show fragment CommentDialog
            FragmentManager fm = CommentListFragment.this.getActivity()
                .getSupportFragmentManager();
            CommentDialogFragment commentDialogFragment =
                CommentDialogFragment.newInstanceStoreComment(storeCommentId, storeName);
            commentDialogFragment.setCommentDialogCallbackContract(this);

            return commentDialogFragment.lifecycle()
                .doOnSubscribe(() -> {
                  commentDialogFragment.show(fm, "fragment_comment_dialog");
                })
                .filter(event -> event.equals(FragmentEvent.DESTROY_VIEW))
                .toCompletable();
          }

          return showSignInMessage();
        });
  }

  @Override public void okSelected(BaseV7Response response, long longAsId, Long previousCommentId,
      String idAsString) {
    if (response instanceof SetComment) {
      ComplexComment complexComment = getComplexComment(((SetComment) response).getData()
          .getBody(), previousCommentId, ((SetComment) response).getData()
          .getId());

      CommentDisplayable commentDisplayable =
          new CommentDisplayable(complexComment, getFragmentNavigator(),
              ((AptoideApplication) getContext().getApplicationContext()).getFragmentProvider());

      if (complexComment.getParent() != null) {
        insertChildCommentInsideParent(complexComment);
      } else {
        addDisplayable(0, commentDisplayable, true);
      }
      ManagerPreferences.setForceServerRefreshFlag(true, sharedPreferences);
      ShowMessage.asSnack(this.getActivity(), R.string.comment_submitted);
    }
  }

  private void insertChildCommentInsideParent(ComplexComment complexComment) {
    displayables.clear();
    boolean added = false;
    ArrayList<Displayable> displayables = new ArrayList<>(comments.size() + 1);
    for (CommentNode commentNode : comments) {
      displayables.add(new CommentDisplayable(new ComplexComment(commentNode,
          createNewCommentFragment(elementIdAsLong, commentNode.getComment()
              .getId(), storeName)), getFragmentNavigator(),
          ((AptoideApplication) getContext().getApplicationContext()).getFragmentProvider()));
      if (commentNode.getComment()
          .getId() == complexComment.getParent()
          .getId() && !added) {
        displayables.add(new CommentDisplayable(complexComment, getFragmentNavigator(),
            ((AptoideApplication) getContext().getApplicationContext()).getFragmentProvider()));
        added = true;
      }
    }
    this.displayables = new ArrayList<>(displayables.size());
    this.displayables.add(new DisplayableGroup(displayables,
        (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE),
        getContext().getResources()));
    clearDisplayables();
    addDisplayables(this.displayables);
  }

  @NonNull
  private ComplexComment getComplexComment(String inputText, Long previousCommentId, long id) {
    Comment comment = new Comment();
    Comment.User user = new Comment.User();
    if (!TextUtils.isEmpty(accountManager.getAccount()
        .getAvatar())) {
      user.setAvatar(accountManager.getAccount()
          .getAvatar());
    } else {
      if (!TextUtils.isEmpty(accountManager.getAccount()
          .getStore()
          .getAvatar())) {
        user.setAvatar(accountManager.getAccount()
            .getStore()
            .getAvatar());
      }
    }
    user.setName(accountManager.getAccount()
        .getNickname());
    comment.setUser(user);
    comment.setBody(inputText);
    comment.setAdded(new Date());
    comment.setId(id);
    CommentNode commentNode = new CommentNode(comment);
    if (previousCommentId != null) {
      Comment.Parent parent = new Comment.Parent();
      parent.setId(previousCommentId);
      comment.setParent(parent);
      commentNode.setLevel(2);
    }
    return new ComplexComment(commentNode, createNewCommentFragment(elementIdAsLong,
        commentNode.getComment()
            .getId(), storeName));
  }
}
