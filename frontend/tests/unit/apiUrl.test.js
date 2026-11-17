import { test } from 'node:test'
import assert from 'node:assert/strict'
import { resolverBaseApi } from '../../src/services/apiUrl.js'
test('proxy local e URL explícita preservam /api', () => {
 assert.equal(resolverBaseApi(), '/api')
 assert.equal(resolverBaseApi('http://localhost:8080'), 'http://localhost:8080/api')
})
test('Render usa prefixo único no cadastro e login', () => {
 for (const base of ['https://cyberladies-api.onrender.com','https://cyberladies-api.onrender.com/api','https://cyberladies-api.onrender.com/api/']) {
  for (const rota of ['/auth/cadastro/usuaria','/auth/login']) assert.equal(resolverBaseApi(base)+rota, 'https://cyberladies-api.onrender.com/api'+rota)
 }
})
test('recusa prefixo duplicado e credenciais', () => {
 for (const base of ['https://example.invalid/api/api','https://user:password@example.invalid/api']) assert.throws(()=>resolverBaseApi(base))
})
