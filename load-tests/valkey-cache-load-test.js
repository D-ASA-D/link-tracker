import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8081";

const USERS = Number(__ENV.USERS || "8");
const CHAT_ID_START = Number(__ENV.CHAT_ID_START || "900000000");
const CHAT_COUNT = Number(__ENV.CHAT_COUNT || "1000");

const GETS_PER_MUTATION = Number(__ENV.GETS_PER_MUTATION || "100");
const REQUEST_SLEEP_SECONDS = Number(__ENV.REQUEST_SLEEP_SECONDS || "0.1");

const getLinks200 = new Counter("get_links_200");
const getLinks500 = new Counter("get_links_500");
const getLinks502504 = new Counter("get_links_502_504");

const postLinks200 = new Counter("post_links_200");
const postLinks500 = new Counter("post_links_500");
const postLinks502504 = new Counter("post_links_502_504");

const deleteLinks200 = new Counter("delete_links_200");
const deleteLinks500 = new Counter("delete_links_500");
const deleteLinks502504 = new Counter("delete_links_502_504");

const getLinksFailed = new Rate("get_links_failed");
const postLinksFailed = new Rate("post_links_failed");
const deleteLinksFailed = new Rate("delete_links_failed");

export const options = {
  scenarios: {
    cache_load_test: {
      executor: "ramping-vus",
      stages: [
        { duration: "1m", target: USERS },
        { duration: "5m", target: USERS },
        { duration: "30s", target: 0 },
      ],
    },
  },

  thresholds: {
    http_req_failed: ["rate<0.05"],

    "http_req_duration{endpoint:GET /links}": ["p(50)<500", "p(99)<3000"],
    "http_req_duration{endpoint:POST /links}": ["p(50)<1000", "p(99)<5000"],
    "http_req_duration{endpoint:DELETE /links}": ["p(50)<1000", "p(99)<5000"],

    get_links_failed: ["rate<0.05"],
    post_links_failed: ["rate<0.10"],
    delete_links_failed: ["rate<0.10"],
  },

  summaryTrendStats: ["avg", "med", "p(50)", "p(95)", "p(99)", "min", "max"],
};

function chatIdForCurrentVu() {
  return CHAT_ID_START + (__VU % CHAT_COUNT);
}

function commonHeaders(chatId) {
  return {
    "Content-Type": "application/json",
    "Tg-Chat-Id": String(chatId),
  };
}

function registerChat(chatId) {
  const response = http.post(`${BASE_URL}/tg-chat/${chatId}`, null, {
    tags: {
      endpoint: "POST /tg-chat/{id}",
    },
  });

  return response.status === 200 || response.status === 409 || response.status === 400;
}

function getLinks(chatId) {
  const response = http.get(`${BASE_URL}/links`, {
    headers: {
      "Tg-Chat-Id": String(chatId),
    },
    tags: {
      endpoint: "GET /links",
    },
  });

  countGetStatus(response);

  const success = response.status === 200;
  getLinksFailed.add(!success);

  check(response, {
    "GET /links status is 200": (r) => r.status === 200,
  });

  return response;
}

function addLink(chatId) {
  const unique = `${chatId}-${__VU}-${__ITER}-${Date.now()}-${Math.floor(Math.random() * 1000000)}`;

  const body = JSON.stringify({
    link: `https://github.com/D-ASA-D/load-test-${unique}`,
    tags: ["load"],
    filters: [],
  });

  const response = http.post(`${BASE_URL}/links`, body, {
    headers: commonHeaders(chatId),
    tags: {
      endpoint: "POST /links",
    },
  });

  countPostStatus(response);

  const success = response.status === 200 || response.status === 409;
  postLinksFailed.add(!success);

  check(response, {
    "POST /links status is 200 or 409": (r) => r.status === 200 || r.status === 409,
  });

  if (response.status === 200) {
    return JSON.parse(response.body).url;
  }

  return null;
}

function deleteLink(chatId, url) {
  if (!url) {
    return;
  }

  const body = JSON.stringify({
    link: url,
  });

  const response = http.del(`${BASE_URL}/links`, body, {
    headers: commonHeaders(chatId),
    tags: {
      endpoint: "DELETE /links",
    },
  });

  countDeleteStatus(response);

  const success = response.status === 200 || response.status === 404;
  deleteLinksFailed.add(!success);

  check(response, {
    "DELETE /links status is 200 or 404": (r) => r.status === 200 || r.status === 404,
  });
}

function countGetStatus(response) {
  if (response.status === 200) {
    getLinks200.add(1);
  }

  if (response.status === 500) {
    getLinks500.add(1);
  }

  if (response.status === 502 || response.status === 504) {
    getLinks502504.add(1);
  }
}

function countPostStatus(response) {
  if (response.status === 200) {
    postLinks200.add(1);
  }

  if (response.status === 500) {
    postLinks500.add(1);
  }

  if (response.status === 502 || response.status === 504) {
    postLinks502504.add(1);
  }
}

function countDeleteStatus(response) {
  if (response.status === 200) {
    deleteLinks200.add(1);
  }

  if (response.status === 500) {
    deleteLinks500.add(1);
  }

  if (response.status === 502 || response.status === 504) {
    deleteLinks502504.add(1);
  }
}

export default function () {
  const chatId = chatIdForCurrentVu();

  registerChat(chatId);

  for (let i = 0; i < GETS_PER_MUTATION; i += 1) {
    getLinks(chatId);
    sleep(REQUEST_SLEEP_SECONDS);
  }

  const addedUrl = addLink(chatId);
  sleep(REQUEST_SLEEP_SECONDS);

  for (let i = 0; i < GETS_PER_MUTATION; i += 1) {
    getLinks(chatId);
    sleep(REQUEST_SLEEP_SECONDS);
  }

  deleteLink(chatId, addedUrl);
  sleep(REQUEST_SLEEP_SECONDS);
}
