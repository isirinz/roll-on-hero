package com.trialmobile.myfantasy.ads;

/**
 * 보상형 광고 연결점. 게임(core)은 이 인터페이스만 알고, 실제 광고 SDK(AdMob 등)는
 * 플랫폼 런처(AndroidLauncher, IOSLauncher)에서 구현해 {@code MainGame.setRewardedAds()}로 넣는다.
 * <p>
 * 광고 SDK 콜백은 보통 렌더 스레드가 아닌 곳에서 오므로, 구현은 결과를 그냥 {@link Callback}에 넘기면 되고
 * 게임 쪽에서 {@code Gdx.app.postRunnable}로 렌더 스레드에 옮겨 처리한다.
 * <p>
 * 광고를 아직 붙이지 않은 지금은 {@link #NONE}(광고 없이 바로 보상)이 기본값이다.
 */
public interface RewardedAds {
    /** 지금 광고를 보여 줄 수 있는지(로드 완료). false면 버튼을 잠시 비활성화한다. */
    boolean isReady();

    /** 광고를 보여 준다. 끝까지 보면 {@link Callback#onRewarded()}, 아니면 {@link Callback#onCancelled()}. */
    void show(String placement, Callback callback);

    interface Callback {
        void onRewarded();

        void onCancelled();
    }

    /** 광고 없이 즉시 보상하는 기본 구현(개발·광고 연동 전). */
    RewardedAds NONE = new RewardedAds() {
        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void show(String placement, Callback callback) {
            callback.onRewarded();
        }
    };
}
