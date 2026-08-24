//review-start，總共1次修改，第1次//
/** 無後端階段的公開商品評價展示資料。 */
const stars = [
  { starId: 301, orderId: 20260824001, orderItemId: 1, productId: 1, productName: 'DinoGo 精選商品', productImage: '', memberId: 1, fiveStar: 5, feedback: '包裝完整，商品質感很好，出貨速度也很快。', imgOne: null, imgTwo: null, imgThree: null, reviewPriority: 1, starUpdAt: '2026-08-24T11:00:00' },
  { starId: 302, orderId: 20260824001, orderItemId: 2, productId: 1, productName: '生活選物組', productImage: '', memberId: 28, fiveStar: 4, feedback: '整體符合期待，會考慮再次購買。', imgOne: null, imgTwo: null, imgThree: null, reviewPriority: 2, starUpdAt: '2026-08-23T15:30:00' },
]

const response = (data) => Promise.resolve({ data })

export function getOrderStars(orderId) {
  return response(stars
    .filter((star) => String(star.orderId) === String(orderId))
    .map((star) => ({ ...star, reviewed: Number(star.fiveStar ?? 0) > 0 })))
}

export function updateStar(starId, request) {
  let star = stars.find((item) => String(item.starId) === String(starId))
  if (!star) {
    star = { starId: Number(starId), productId: 1, memberId: 1, reviewPriority: 1 }
    stars.push(star)
  }
  Object.assign(star, request, {
    reviewed: Number(request.fiveStar ?? 0) > 0,
    starUpdAt: new Date().toISOString(),
  })
  return response(star)
}

export function clearStar(starId) {
  const star = stars.find((item) => String(item.starId) === String(starId))
  if (star) Object.assign(star, { fiveStar: 0, reviewed: false, feedback: '', imgOne: null, imgTwo: null, imgThree: null })
  return response(star)
}

export function getProductReviews(productId) {
  const content = stars.filter((star) => String(star.productId) === String(productId) && star.fiveStar > 0)
  return response({ content, hasNext: false, nextCursor: null })
}
//review-end，總共1次修改，第1次//
