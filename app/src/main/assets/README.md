# Lottie Animation Files Required

This app uses Lottie animations for a premium user experience. 

## Required Animation Files

Place the following Lottie JSON files in `app/src/main/res/raw/`:

1. **loading_animation.json** - Loading spinner animation
   - Recommended: Circular loader with subtle animation
   - Duration: 2-3 seconds loop

2. **empty_state.json** - Empty state illustration
   - Recommended: Cute character with folder/file illustration
   - Duration: 3-5 seconds loop

3. **onboarding_welcome.json** - Welcome screen animation
   - Recommended: App mascot or friendly character greeting
   - Duration: 3-4 seconds loop

4. **onboarding_how_it_works.json** - How it works illustration
   - Recommended: WhatsApp status scanning animation
   - Duration: 3-4 seconds loop

5. **onboarding_save.json** - Save status illustration
   - Recommended: Download/save action animation
   - Duration: 3-4 seconds loop

6. **onboarding_vault.json** - Vault/security illustration
   - Recommended: Lock/vault with security theme
   - Duration: 3-4 seconds loop

7. **save_success.json** - Save success feedback
   - Recommended: Checkmark or success tick animation
   - Duration: 1-2 seconds

8. **refresh.json** - Pull to refresh animation
   - Recommended: Circular refresh arrow
   - Duration: 1-2 seconds loop

## Where to Get Lottie Animations

- **LottieFiles**: https://lottiefiles.com (Free and premium animations)
- **IconScout**: https://iconscout.com/lottie-animations
- **Create Custom**: Use Adobe After Effects with Bodymovin plugin

## Recommended Animations (Free on LottieFiles)

1. Loading: Search "loading spinner" or "circular loader"
2. Empty State: Search "empty state" or "no data"
3. Welcome: Search "welcome" or "greeting"
4. Download: Search "download" or "save"
5. Security: Search "lock" or "vault" or "security"

## Animation Guidelines

- Keep file sizes small (< 50KB preferred)
- Use loop for loading and empty states
- Use one-shot for success/error feedback
- Match app color scheme when possible
- Use smooth easing (no harsh movements)
