import {
  getTimelineWindow,
  networkBeforeUiFailure,
  sortTimelineEvents,
  ContextEvent,
} from './context';

describe('context timeline', () => {
  const events = sortTimelineEvents([
    {
      kind: 'network',
      timestamp_ms: 1000,
      trace_id: 't1',
      request: {
        method: 'POST',
        url: 'http://localhost/api/payment-intent',
        phase: 'failed',
        error_text: 'timeout',
      },
    },
    {
      kind: 'ui',
      timestamp_ms: 1500,
      trace_id: 't1',
      element: '[data-testid=pay-btn]',
      action: 'not_found',
    },
  ] as ContextEvent[]);

  it('filters window around failure', () => {
    expect(getTimelineWindow({ events }, 1500, 5000)).toHaveLength(2);
  });

  it('detects network failure before UI', () => {
    expect(networkBeforeUiFailure({ events }, 1500, 5000)).toBe(true);
  });
});
