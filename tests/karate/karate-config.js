function fn() {
  var env = karate.env || 'dev';
  karate.log('Karate environment running on:', env);
  
  var config = {
    appUrl: 'https://ais-dev-ysij5gfggpt2akxlk7vm3b-58584043488.asia-east1.run.app',
    firebaseProjectId: 'vibesync-chat-social-connect',
    isSparkPlan: true,
    expectedMaxLatencyMs: 300
  };

  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 5000);
  karate.configure('logPrettyRequest', true);
  karate.configure('logPrettyResponse', true);

  return config;
}
