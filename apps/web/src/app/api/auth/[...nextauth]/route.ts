import { NextRequest, NextResponse } from 'next/server';
import { handlers } from '@/auth';

export const GET = handlers.GET;
export const POST = handlers.POST;

export function OPTIONS(_request: NextRequest) {
  return new NextResponse(null, { status: 204 });
}
